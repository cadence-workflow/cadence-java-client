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
 * Defines what happens when a new scheduled run is triggered while a previous one is still running.
 *
 * <p>Behavior is not retroactive on update: existing runs keep running under the old policy; only
 * new fires observe the updated policy. See {@link
 * com.uber.cadence.client.ScheduleClient#updateSchedule} for detail.
 */
public enum ScheduleOverlapPolicy {
  /**
   * Skip the new run if the previous workflow is still running. This is the default.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_SKIP_NEW}.
   */
  SKIP_NEW,

  /**
   * Buffer new runs and execute them sequentially after the current run completes.
   *
   * <p>The maximum queue depth is controlled by {@link SchedulePolicies#getBufferLimit()}. A limit
   * of 0 means unlimited. When updating from BUFFER to any other policy the queued runs are
   * dropped.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_BUFFER}.
   */
  BUFFER,

  /**
   * Allow multiple runs to execute concurrently with no ordering guarantee.
   *
   * <p>The maximum concurrency is controlled by {@link SchedulePolicies#getConcurrencyLimit()}. A
   * limit of 0 means unlimited.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_CONCURRENT}.
   */
  CONCURRENT,

  /**
   * Cancel the previous run gracefully, then start the new one.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_CANCEL_PREVIOUS}.
   */
  CANCEL_PREVIOUS,

  /**
   * Terminate the previous run immediately, then start the new one.
   *
   * <p>Equivalent to proto {@code SCHEDULE_OVERLAP_POLICY_TERMINATE_PREVIOUS}.
   */
  TERMINATE_PREVIOUS,
}
