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

import com.uber.cadence.ScheduleOverlapPolicy;
import java.time.Instant;
import java.util.Objects;

/** A time range to trigger retroactively when calling {@link ScheduleHandle#backfill}. */
public final class ScheduleBackfill {

  private final Instant startTime;
  private final Instant endTime;
  private final ScheduleOverlapPolicy overlapPolicy;

  /**
   * @param startTime start of the backfill range (inclusive)
   * @param endTime end of the backfill range (inclusive)
   * @param overlapPolicy how to handle overlapping runs; use {@link ScheduleOverlapPolicy#INVALID}
   *     to fall back to the schedule's configured policy
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
}
