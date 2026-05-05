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
import com.uber.m3.util.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Histogram bucket configurations for timer metrics migration.
 *
 * <p>This class defines standard histogram bucket configurations used during the migration from
 * timers to histograms. These buckets provide consistent granularity for measuring latencies across
 * different time ranges.
 *
 * <p>Note: Unlike the Go client which uses subsettable exponential histograms with algorithmic
 * bucket generation, the Java client uses explicit bucket definitions. We provide multiple
 * configurations to balance between granularity and cardinality:
 *
 * <ul>
 *   <li><b>DEFAULT_1MS_100S</b>: Most common metrics (46 buckets, 1ms-100s)
 *   <li><b>LOW_1MS_100S</b>: High-cardinality metrics (16 buckets, 1ms-100s)
 *   <li><b>HIGH_1MS_24H</b>: Long-running operations (27 buckets, 1ms-24h)
 *   <li><b>MID_1MS_24H</b>: High-cardinality long operations (14 buckets, 1ms-24h)
 * </ul>
 */
public final class HistogramBuckets {

  /**
   * Default bucket configuration for most client-side latency metrics.
   *
   * <p>Range: 1ms to 100s
   *
   * <p>Provides: - Fine-grained buckets (1ms steps) from 1ms to 10ms - Medium-grained buckets (10ms
   * steps) from 10ms to 100ms - Coarser buckets (100ms steps) from 100ms to 1s - Second-level
   * buckets from 1s to 100s
   *
   * <p>Use for: - Decision poll latency - Activity poll latency - Decision execution latency -
   * Activity execution latency - Workflow replay latency - Most RPC call latencies
   */
  public static final DurationBuckets DEFAULT_1MS_100S =
      DurationBuckets.custom(
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(1)), // 1ms
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(3)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(4)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(6)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(7)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(8)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(9)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(30)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(40)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(50)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(60)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(70)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(80)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(90)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(100)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(200)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(300)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(400)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(500)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(600)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(700)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(800)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(900)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(3)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(4)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(6)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(7)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(8)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(9)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(30)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(40)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(50)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(60)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(70)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(80)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(90)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(100)));

  /**
   * Low-resolution bucket configuration for high-cardinality metrics.
   *
   * <p>Range: 1ms to 100s (same as DEFAULT_1MS_100S but with fewer buckets)
   *
   * <p>Provides: - Coarser buckets with ~2x steps instead of fine-grained steps - Approximately
   * half the cardinality of DEFAULT_1MS_100S
   *
   * <p>Use for: - Per-activity-type metrics where cardinality is high - Per-workflow-type metrics
   * where cardinality is high - Metrics with many tag combinations
   */
  public static final DurationBuckets LOW_1MS_100S =
      DurationBuckets.custom(
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(50)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(100)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(200)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(500)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(50)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(100)));

  /**
   * High-resolution bucket configuration for long-running operations.
   *
   * <p>Range: 1ms to 24 hours
   *
   * <p>Provides: - Fine-grained buckets from 1ms to 10ms - Medium-grained from 10ms to 1s -
   * Second-level buckets from 1s to 10 minutes - Minute-level buckets from 10 minutes to 24 hours
   *
   * <p>Use for: - Workflow end-to-end latency - Long-running activity execution latency - Multi-day
   * operation metrics
   */
  public static final DurationBuckets HIGH_1MS_24H =
      DurationBuckets.custom(
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(50)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(100)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(200)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(500)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(2)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(5)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(20)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(30)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(60)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(120)), // 2 min
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(300)), // 5 min
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(600)), // 10 min
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(20)),
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(30)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(1)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(2)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(4)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(8)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(12)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(24)));

  /**
   * Medium-resolution bucket configuration for long-running operations.
   *
   * <p>Range: 1ms to 24 hours (same as HIGH_1MS_24H but with fewer buckets)
   *
   * <p>Provides: - Coarser buckets than HIGH_1MS_24H - Better for high-cardinality long-duration
   * metrics
   *
   * <p>Use for: - When HIGH_1MS_24H's cardinality is too high - Per-workflow-type E2E latency with
   * many workflow types
   */
  public static final DurationBuckets MID_1MS_24H =
      DurationBuckets.custom(
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(100)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(1)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(10)),
          Duration.ofNanos(TimeUnit.SECONDS.toNanos(30)),
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(1)),
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(5)),
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(10)),
          Duration.ofNanos(TimeUnit.MINUTES.toNanos(30)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(1)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(4)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(12)),
          Duration.ofNanos(TimeUnit.HOURS.toNanos(24)));

  private HistogramBuckets() {
    // Utility class - prevent instantiation
  }
}
