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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.internal.metrics.MetricsEmit.MetricEmitMode;
import com.uber.m3.tally.Histogram;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.tally.Timer;
import com.uber.m3.util.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetricsEmitTest {

  private Scope mockScope;
  private Timer mockTimer;
  private Histogram mockHistogram;
  private MetricEmitMode originalMode;

  @Before
  public void setUp() {
    mockScope = mock(Scope.class);
    mockTimer = mock(Timer.class);
    mockHistogram = mock(Histogram.class);
    // Save original mode to restore after each test
    originalMode = MetricsEmit.getEmitMode();
    // Reset to default mode for each test
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_BOTH);
  }

  @After
  public void tearDown() {
    // Restore original mode after each test
    MetricsEmit.setEmitMode(originalMode);
  }

  @Test
  public void testEmitLatency_DualEmit() {
    when(mockScope.timer("test-metric")).thenReturn(mockTimer);
    when(mockScope.histogram(eq("test-metric_ns"), any())).thenReturn(mockHistogram);

    Duration latency = Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(150));
    MetricsEmit.emitLatency(mockScope, "test-metric", latency, HistogramBuckets.DEFAULT_1MS_100S);

    // Verify both timer and histogram received the latency
    verify(mockTimer, times(1)).record(latency);
    verify(mockHistogram, times(1)).recordDuration(latency);
  }

  @Test
  public void testStartLatency_DualEmit() {
    Stopwatch mockTimerSW = mock(Stopwatch.class);
    Stopwatch mockHistogramSW = mock(Stopwatch.class);

    when(mockScope.timer("test-metric")).thenReturn(mockTimer);
    when(mockTimer.start()).thenReturn(mockTimerSW);
    when(mockScope.histogram(eq("test-metric_ns"), any())).thenReturn(mockHistogram);
    when(mockHistogram.start()).thenReturn(mockHistogramSW);

    MetricsEmit.DualStopwatch sw =
        MetricsEmit.startLatency(mockScope, "test-metric", HistogramBuckets.DEFAULT_1MS_100S);
    sw.stop();

    // Verify both stopwatches were started and stopped
    verify(mockTimer, times(1)).start();
    verify(mockHistogram, times(1)).start();
    verify(mockTimerSW, times(1)).stop();
    verify(mockHistogramSW, times(1)).stop();
  }

  @Test
  public void testStartLatency_StopNotCalledTwice() {
    Stopwatch mockTimerSW = mock(Stopwatch.class);
    Stopwatch mockHistogramSW = mock(Stopwatch.class);

    when(mockScope.timer("test-metric")).thenReturn(mockTimer);
    when(mockTimer.start()).thenReturn(mockTimerSW);
    when(mockScope.histogram(eq("test-metric_ns"), any())).thenReturn(mockHistogram);
    when(mockHistogram.start()).thenReturn(mockHistogramSW);

    MetricsEmit.DualStopwatch sw =
        MetricsEmit.startLatency(mockScope, "test-metric", HistogramBuckets.DEFAULT_1MS_100S);

    // Stop is not called yet
    verify(mockTimerSW, never()).stop();
    verify(mockHistogramSW, never()).stop();

    sw.stop();

    // Now stop should be called once on each
    verify(mockTimerSW, times(1)).stop();
    verify(mockHistogramSW, times(1)).stop();
  }

  @Test
  public void testHistogramBuckets_Default1ms100s_NotNull() {
    assertNotNull("DEFAULT_1MS_100S buckets should not be null", HistogramBuckets.DEFAULT_1MS_100S);
  }

  @Test
  public void testHistogramBuckets_Low1ms100s_NotNull() {
    assertNotNull("LOW_1MS_100S buckets should not be null", HistogramBuckets.LOW_1MS_100S);
  }

  @Test
  public void testHistogramBuckets_High1ms24h_NotNull() {
    assertNotNull("HIGH_1MS_24H buckets should not be null", HistogramBuckets.HIGH_1MS_24H);
  }

  @Test
  public void testHistogramBuckets_Mid1ms24h_NotNull() {
    assertNotNull("MID_1MS_24H buckets should not be null", HistogramBuckets.MID_1MS_24H);
  }

  @Test
  public void testEmitMode_Default() {
    // Default mode should be EMIT_BOTH
    assertEquals(MetricEmitMode.EMIT_BOTH, MetricsEmit.getEmitMode());
  }

  @Test
  public void testSetEmitMode() {
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_TIMERS_ONLY);
    assertEquals(MetricEmitMode.EMIT_TIMERS_ONLY, MetricsEmit.getEmitMode());

    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_HISTOGRAMS_ONLY);
    assertEquals(MetricEmitMode.EMIT_HISTOGRAMS_ONLY, MetricsEmit.getEmitMode());

    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_BOTH);
    assertEquals(MetricEmitMode.EMIT_BOTH, MetricsEmit.getEmitMode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmitMode_Null() {
    MetricsEmit.setEmitMode(null);
  }

  @Test
  public void testEmitLatency_TimersOnly() {
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_TIMERS_ONLY);

    when(mockScope.timer("test-metric")).thenReturn(mockTimer);

    Duration latency = Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(150));
    MetricsEmit.emitLatency(mockScope, "test-metric", latency, HistogramBuckets.DEFAULT_1MS_100S);

    // Verify only timer received the latency
    verify(mockTimer, times(1)).record(latency);
    verify(mockScope, never()).histogram(anyString(), any());
  }

  @Test
  public void testEmitLatency_HistogramsOnly() {
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_HISTOGRAMS_ONLY);

    when(mockScope.histogram(eq("test-metric_ns"), any())).thenReturn(mockHistogram);

    Duration latency = Duration.ofNanos(TimeUnit.MILLISECONDS.toNanos(150));
    MetricsEmit.emitLatency(mockScope, "test-metric", latency, HistogramBuckets.DEFAULT_1MS_100S);

    // Verify only histogram received the latency
    verify(mockHistogram, times(1)).recordDuration(latency);
    verify(mockScope, never()).timer(anyString());
  }

  @Test
  public void testStartLatency_TimersOnly() {
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_TIMERS_ONLY);

    Stopwatch mockTimerSW = mock(Stopwatch.class);

    when(mockScope.timer("test-metric")).thenReturn(mockTimer);
    when(mockTimer.start()).thenReturn(mockTimerSW);

    MetricsEmit.DualStopwatch sw =
        MetricsEmit.startLatency(mockScope, "test-metric", HistogramBuckets.DEFAULT_1MS_100S);
    sw.stop();

    // Verify only timer stopwatch was started and stopped
    verify(mockTimer, times(1)).start();
    verify(mockTimerSW, times(1)).stop();
    verify(mockScope, never()).histogram(anyString(), any());
  }

  @Test
  public void testStartLatency_HistogramsOnly() {
    MetricsEmit.setEmitMode(MetricEmitMode.EMIT_HISTOGRAMS_ONLY);

    Stopwatch mockHistogramSW = mock(Stopwatch.class);

    when(mockScope.histogram(eq("test-metric_ns"), any())).thenReturn(mockHistogram);
    when(mockHistogram.start()).thenReturn(mockHistogramSW);

    MetricsEmit.DualStopwatch sw =
        MetricsEmit.startLatency(mockScope, "test-metric", HistogramBuckets.DEFAULT_1MS_100S);
    sw.stop();

    // Verify only histogram stopwatch was started and stopped
    verify(mockHistogram, times(1)).start();
    verify(mockHistogramSW, times(1)).stop();
    verify(mockScope, never()).timer(anyString());
  }
}
