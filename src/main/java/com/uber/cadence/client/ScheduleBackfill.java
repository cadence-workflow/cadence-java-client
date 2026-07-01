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

package com.uber.cadence.client;

import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import java.time.Instant;
import java.util.Objects;

/** A time range to trigger retroactively when calling {@link ScheduleClient#backfillSchedule}. */
public final class ScheduleBackfill {

  private final Instant startTime;
  private final Instant endTime;
  private final ScheduleOverlapPolicy overlapPolicy;

  /**
   * Uses the schedule's configured overlap policy for this backfill range.
   *
   * @param startTime start of the backfill range (inclusive)
   * @param endTime end of the backfill range (inclusive)
   */
  public ScheduleBackfill(Instant startTime, Instant endTime) {
    this.startTime = Objects.requireNonNull(startTime, "startTime");
    this.endTime = Objects.requireNonNull(endTime, "endTime");
    this.overlapPolicy = null;
  }

  /**
   * @param startTime start of the backfill range (inclusive)
   * @param endTime end of the backfill range (inclusive)
   * @param overlapPolicy overlap policy for this backfill range; overrides the schedule's
   *     configured policy
   */
  public ScheduleBackfill(Instant startTime, Instant endTime, ScheduleOverlapPolicy overlapPolicy) {
    this.startTime = Objects.requireNonNull(startTime, "startTime");
    this.endTime = Objects.requireNonNull(endTime, "endTime");
    this.overlapPolicy = Objects.requireNonNull(overlapPolicy, "overlapPolicy");
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public ScheduleOverlapPolicy getOverlapPolicy() {
    return overlapPolicy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ScheduleBackfill)) return false;
    ScheduleBackfill that = (ScheduleBackfill) o;
    return Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && overlapPolicy == that.overlapPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime, overlapPolicy);
  }

  @Override
  public String toString() {
    return "ScheduleBackfill{"
        + "startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", overlapPolicy="
        + overlapPolicy
        + '}';
  }
}
