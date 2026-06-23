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

package com.uber.cadence.reporter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.m3.tally.Capabilities;
import com.uber.m3.tally.CapableOf;
import com.uber.m3.tally.StatsReporter;
import com.uber.m3.util.Duration;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CadenceClientStatsReporter implements StatsReporter {

  private final Map<String, AtomicDouble> gauges = new ConcurrentHashMap<>();

  @Override
  public Capabilities capabilities() {
    return CapableOf.REPORTING;
  }

  @Override
  public void flush() {
    // NOOP
  }

  @Override
  public void close() {
    // NOOP
  }

  @Override
  public void reportCounter(String name, Map<String, String> tags, long value) {
    Metrics.counter(name, getTags(tags)).increment(value);
  }

  @Override
  public void reportGauge(String name, Map<String, String> tags, double value) {
    AtomicDouble gauge =
        gauges.computeIfAbsent(
            name,
            metricName -> {
              AtomicDouble result = Metrics.gauge(name, getTags(tags), new AtomicDouble());
              Preconditions.checkNotNull(result, "Metrics.gauge should not return null ever");
              return result;
            });
    gauge.set(value);
  }

  @Override
  public void reportTimer(String name, Map<String, String> tags, Duration interval) {
    Metrics.timer(name, getTags(tags)).record(interval.getNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void reportHistogramValueSamples(
      String name,
      Map<String, String> tags,
      com.uber.m3.tally.Buckets buckets,
      double bucketLowerBound,
      double bucketUpperBound,
      long samples) {
    // NOOP
  }

  @Override
  @SuppressWarnings("deprecation")
  public void reportHistogramDurationSamples(
      String name,
      Map<String, String> tags,
      com.uber.m3.tally.Buckets buckets,
      Duration bucketLowerBound,
      Duration bucketUpperBound,
      long samples) {
    io.micrometer.core.instrument.Timer timer = Metrics.timer(name, getTags(tags));
    for (long i = 0; i < samples; i++) {
      timer.record(bucketUpperBound.getNanos(), TimeUnit.NANOSECONDS);
    }
  }

  private Iterable<Tag> getTags(Map<String, String> tags) {
    // TODO: not sure why we have to fill empty values, maybe we can remove them
    final HashMap<String, String> tmpMap = new HashMap<>(tags);
    tmpMap.put(MetricsTag.ACTIVITY_TYPE, Strings.nullToEmpty(tags.get(MetricsTag.ACTIVITY_TYPE)));
    tmpMap.put(MetricsTag.DOMAIN, Strings.nullToEmpty(tags.get(MetricsTag.DOMAIN)));
    tmpMap.put(MetricsTag.TASK_LIST, Strings.nullToEmpty(tags.get(MetricsTag.TASK_LIST)));
    tmpMap.put(MetricsTag.WORKFLOW_TYPE, Strings.nullToEmpty(tags.get(MetricsTag.WORKFLOW_TYPE)));

    final ImmutableList.Builder<Tag> builder = ImmutableList.<Tag>builder();
    for (Map.Entry<String, String> entry : tmpMap.entrySet()) {
      final Tag tag = Tag.of(entry.getKey(), entry.getValue());
      builder.add(tag);
    }
    return builder.build();
  }
}
