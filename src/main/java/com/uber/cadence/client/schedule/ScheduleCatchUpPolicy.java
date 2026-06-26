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

/**
 * Defines how missed runs are handled when a schedule is unpaused or when the server recovers from
 * downtime.
 *
 * <p>Catch-up runs are still subject to the configured {@link ScheduleOverlapPolicy}: if the buffer
 * or concurrency limit is reached, excess catch-up runs are dropped.
 *
 * <p>The catch-up window (maximum look-back horizon) is configured separately via {@link
 * SchedulePolicies#getCatchUpWindow()}.
 */
public enum ScheduleCatchUpPolicy {
  /**
   * Skip all missed runs. Only future runs will execute.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_SKIP}.
   */
  SKIP,

  /**
   * Execute only the single most-recently missed scheduled time, skipping all others.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_ONE}.
   */
  ONE,

  /**
   * Execute a run for every missed scheduled time within the catch-up window.
   *
   * <p>Equivalent to proto {@code SCHEDULE_CATCH_UP_POLICY_ALL}.
   */
  ALL,
}
