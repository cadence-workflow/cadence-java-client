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

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a schedule's pause state returned by {@link
 * com.uber.cadence.client.ScheduleClient#describeSchedule}.
 */
public final class ScheduleState {

  private final boolean paused;
  private final String pauseReason;
  private final Instant pausedAt;
  private final String pausedBy;

  public ScheduleState(boolean paused, String pauseReason, Instant pausedAt, String pausedBy) {
    this.paused = paused;
    this.pauseReason = pauseReason;
    this.pausedAt = pausedAt;
    this.pausedBy = pausedBy;
  }

  /** Whether the schedule is currently paused. */
  public boolean isPaused() {
    return paused;
  }

  /** The reason provided when the schedule was paused. {@code null} when not paused. */
  public String getPauseReason() {
    return pauseReason;
  }

  /** When the schedule was paused. {@code null} when not paused. */
  public Instant getPausedAt() {
    return pausedAt;
  }

  /** Identity of the actor that paused the schedule. {@code null} when not paused. */
  public String getPausedBy() {
    return pausedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleState)) return false;
    ScheduleState that = (ScheduleState) o;
    return paused == that.paused
        && Objects.equals(pauseReason, that.pauseReason)
        && Objects.equals(pausedAt, that.pausedAt)
        && Objects.equals(pausedBy, that.pausedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paused, pauseReason, pausedAt, pausedBy);
  }

  @Override
  public String toString() {
    return "ScheduleState{"
        + "paused="
        + paused
        + ", pauseReason='"
        + pauseReason
        + "', pausedAt="
        + pausedAt
        + ", pausedBy='"
        + pausedBy
        + "'}";
  }
}
