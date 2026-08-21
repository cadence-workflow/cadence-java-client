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

import java.util.Objects;

/**
 * Initial pause state supplied on {@link
 * com.uber.cadence.client.ScheduleClient#createSchedule(String, ScheduleSpec, ScheduleAction,
 * SchedulePolicies, ScheduleInitialState)} to create a schedule already paused. {@code pausedAt} is
 * server-populated and therefore not accepted as input.
 */
public final class ScheduleInitialState {

  private final boolean paused;
  private final String pauseReason;
  private final String pausedBy;

  public ScheduleInitialState(boolean paused, String pauseReason, String pausedBy) {
    this.paused = paused;
    this.pauseReason = pauseReason;
    this.pausedBy = pausedBy;
  }

  /** Whether to create the schedule in the paused state. */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Human-readable reason for the initial pause. May be {@code null} when {@link #isPaused()} is
   * {@code false}.
   */
  public String getPauseReason() {
    return pauseReason;
  }

  /**
   * Identity of the actor initiating the pause. May be {@code null} when {@link #isPaused()} is
   * {@code false}.
   */
  public String getPausedBy() {
    return pausedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleInitialState)) return false;
    ScheduleInitialState that = (ScheduleInitialState) o;
    return paused == that.paused
        && Objects.equals(pauseReason, that.pauseReason)
        && Objects.equals(pausedBy, that.pausedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paused, pauseReason, pausedBy);
  }

  @Override
  public String toString() {
    return "ScheduleInitialState{"
        + "paused="
        + paused
        + ", pauseReason='"
        + pauseReason
        + "', pausedBy='"
        + pausedBy
        + "'}";
  }
}
