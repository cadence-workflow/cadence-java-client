/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.client.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Defines when a schedule fires.
 *
 * <p>Construct via {@link #newBuilder()}:
 *
 * <pre>{@code
 * ScheduleSpec spec = ScheduleSpec.newBuilder()
 *     .setCronExpression("0 6 * * *")        // daily at 06:00 UTC
 *     .setStartTime(Instant.now())
 *     .setJitter(Duration.ofMinutes(5))
 *     .build();
 * }</pre>
 *
 * <p>The cron timezone can be embedded with a {@code CRON_TZ=<tz>} prefix, e.g. {@code
 * "CRON_TZ=America/Los_Angeles 0 6 * * *"}. If omitted, UTC is assumed.
 */
public final class ScheduleSpec {

  private final String cronExpression;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration jitter;

  private ScheduleSpec(Builder b) {
    this.cronExpression = b.cronExpression;
    this.startTime = b.startTime;
    this.endTime = b.endTime;
    this.jitter = b.jitter;
  }

  /**
   * Standard cron expression, e.g. {@code "0 6 * * *"} (daily at 06:00 UTC).
   *
   * <p>Timezone prefix is supported: {@code "CRON_TZ=America/New_York 0 9 * * 1-5"}.
   */
  public String getCronExpression() {
    return cronExpression;
  }

  /** Earliest time the schedule may fire. {@code null} means start immediately. */
  public Instant getStartTime() {
    return startTime;
  }

  /** Latest time the schedule may fire. {@code null} means run indefinitely. */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Random jitter added to each fire time to spread load. The actual fire time is offset by a
   * random duration uniformly drawn from {@code [0, jitter)}. {@code null} means no jitter.
   */
  public Duration getJitter() {
    return jitter;
  }

  /** Returns a new builder pre-populated with the values from this instance. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Returns a new empty builder. */
  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleSpec)) return false;
    ScheduleSpec that = (ScheduleSpec) o;
    return Objects.equals(cronExpression, that.cronExpression)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(jitter, that.jitter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cronExpression, startTime, endTime, jitter);
  }

  @Override
  public String toString() {
    return "ScheduleSpec{"
        + "cronExpression='"
        + cronExpression
        + "', startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", jitter="
        + jitter
        + '}';
  }

  public static final class Builder {

    private String cronExpression;
    private Instant startTime;
    private Instant endTime;
    private Duration jitter;

    private Builder() {}

    private Builder(ScheduleSpec src) {
      this.cronExpression = src.cronExpression;
      this.startTime = src.startTime;
      this.endTime = src.endTime;
      this.jitter = src.jitter;
    }

    /** @see ScheduleSpec#getCronExpression() */
    public Builder setCronExpression(String cronExpression) {
      this.cronExpression = cronExpression;
      return this;
    }

    /** @see ScheduleSpec#getStartTime() */
    public Builder setStartTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    /** @see ScheduleSpec#getEndTime() */
    public Builder setEndTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    /** @see ScheduleSpec#getJitter() */
    public Builder setJitter(Duration jitter) {
      this.jitter = jitter;
      return this;
    }

    public ScheduleSpec build() {
      return new ScheduleSpec(this);
    }
  }
}
