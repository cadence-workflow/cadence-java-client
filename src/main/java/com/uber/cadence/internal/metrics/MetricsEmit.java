/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.metrics;

import com.uber.m3.tally.DurationBuckets;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.util.Duration;

/**
 * Helper utilities for dual-emitting metrics during timer to histogram migration.
 *
 * <p>This class provides utilities to support gradual migration from timer metrics to histogram
 * metrics. By default, both timer and histogram metrics are emitted to support gradual
 * dashboard/alert migration without requiring a flag day.
 *
 * <p>Migration path: 1. Use these helpers to emit both timers and histograms (default behavior) 2.
 * Update dashboards/alerts to use histogram metrics 3. In a future release, remove timer emission
 * and use histograms exclusively
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Direct latency recording
 * Duration latency = Duration.ofMillis(150);
 * MetricsEmit.emitLatency(scope, MetricsType.DECISION_POLL_LATENCY, latency, HistogramBuckets.DEFAULT_1MS_100S);
 *
 * // Using stopwatch
 * DualStopwatch sw = MetricsEmit.startLatency(scope, MetricsType.ACTIVITY_EXEC_LATENCY, HistogramBuckets.DEFAULT_1MS_100S);
 * // ... do work ...
 * sw.stop();
 *
 * // Configure emission mode (optional, typically done at application startup)
 * MetricsEmit.setEmitMode(MetricEmitMode.EMIT_HISTOGRAMS_ONLY);
 * }</pre>
 */
public final class MetricsEmit {

  /** Metric emission mode controls which metrics are emitted for latency measurements. */
  public enum MetricEmitMode {
    /** Emit only timer metrics (legacy OSS behavior) */
    EMIT_TIMERS_ONLY,
    /** Emit both timer and histogram metrics (default for migration) */
    EMIT_BOTH,
    /** Emit only histogram metrics (post-migration) */
    EMIT_HISTOGRAMS_ONLY
  }

  /**
   * Current emission mode. Default is EMIT_BOTH for migration. This should be set during
   * application initialization (e.g., in static initializer or before starting workers). It should
   * NOT be changed dynamically after workers have started.
   */
  private static volatile MetricEmitMode currentEmitMode = MetricEmitMode.EMIT_BOTH;

  /**
   * Configures the metric emission strategy. This should be called during application
   * initialization, before any metrics are emitted.
   *
   * @param mode The emission mode to use
   *     <p>Example usage:
   *     <pre>{@code
   * // To use only timers (legacy behavior)
   * MetricsEmit.setEmitMode(MetricEmitMode.EMIT_TIMERS_ONLY);
   *
   * // To use both (default, for migration)
   * MetricsEmit.setEmitMode(MetricEmitMode.EMIT_BOTH);
   *
   * // To use only histograms (post-migration)
   * MetricsEmit.setEmitMode(MetricEmitMode.EMIT_HISTOGRAMS_ONLY);
   * }</pre>
   */
  public static void setEmitMode(MetricEmitMode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("MetricEmitMode cannot be null");
    }
    currentEmitMode = mode;
  }

  /**
   * Returns the current emission mode.
   *
   * @return The current emission mode
   */
  public static MetricEmitMode getEmitMode() {
    return currentEmitMode;
  }

  /**
   * Records latency based on the current emit mode setting.
   *
   * <p>This helper function supports flexible metric emission during timer→histogram migration. The
   * behavior depends on the current emit mode:
   *
   * <ul>
   *   <li>EMIT_TIMERS_ONLY: Only timer metrics are emitted
   *   <li>EMIT_BOTH: Both timer and histogram metrics are emitted (default)
   *   <li>EMIT_HISTOGRAMS_ONLY: Only histogram metrics are emitted
   * </ul>
   *
   * @param scope The tally scope to emit metrics to
   * @param name The metric name (without suffix)
   * @param latency The duration to record
   * @param buckets The histogram bucket configuration to use
   */
  public static void emitLatency(
      Scope scope, String name, Duration latency, DurationBuckets buckets) {
    switch (currentEmitMode) {
      case EMIT_TIMERS_ONLY:
        scope.timer(name).record(latency);
        break;
      case EMIT_BOTH:
        scope.timer(name).record(latency);
        scope.histogram(name, buckets).recordDuration(latency);
        break;
      case EMIT_HISTOGRAMS_ONLY:
        scope.histogram(name, buckets).recordDuration(latency);
        break;
    }
  }

  /**
   * Creates a stopwatch that emits based on current emit mode setting.
   *
   * <p>Call .stop() on the returned stopwatch to record the duration. The behavior depends on the
   * current emit mode.
   *
   * @param scope The tally scope to emit metrics to
   * @param name The metric name (without suffix)
   * @param buckets The histogram bucket configuration to use
   * @return A dual stopwatch that records based on emit mode
   */
  public static DualStopwatch startLatency(Scope scope, String name, DurationBuckets buckets) {
    MetricEmitMode mode = currentEmitMode;
    Stopwatch timerSW = null;
    Stopwatch histogramSW = null;

    switch (mode) {
      case EMIT_TIMERS_ONLY:
        timerSW = scope.timer(name).start();
        break;
      case EMIT_BOTH:
        timerSW = scope.timer(name).start();
        histogramSW = scope.histogram(name, buckets).start();
        break;
      case EMIT_HISTOGRAMS_ONLY:
        histogramSW = scope.histogram(name, buckets).start();
        break;
    }

    return new DualStopwatch(timerSW, histogramSW, mode);
  }

  /**
   * A stopwatch that emits metrics based on the emit mode setting.
   *
   * <p>This supports flexible metric emission during timer→histogram migration. The metrics emitted
   * depend on the mode captured when the stopwatch was started.
   */
  public static class DualStopwatch {
    private final Stopwatch timerSW;
    private final Stopwatch histogramSW;
    private final MetricEmitMode mode;

    DualStopwatch(Stopwatch timerSW, Stopwatch histogramSW, MetricEmitMode mode) {
      this.timerSW = timerSW;
      this.histogramSW = histogramSW;
      this.mode = mode;
    }

    /** Stops and records the elapsed time based on emit mode setting. */
    public void stop() {
      switch (mode) {
        case EMIT_TIMERS_ONLY:
          if (timerSW != null) {
            timerSW.stop();
          }
          break;
        case EMIT_BOTH:
          if (timerSW != null) {
            timerSW.stop();
          }
          if (histogramSW != null) {
            histogramSW.stop();
          }
          break;
        case EMIT_HISTOGRAMS_ONLY:
          if (histogramSW != null) {
            histogramSW.stop();
          }
          break;
      }
    }
  }

  private MetricsEmit() {
    // Utility class - prevent instantiation
  }
}
