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

import com.uber.cadence.BackfillInfo;
import com.uber.cadence.CadenceError;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ScheduleAction;
import com.uber.cadence.SchedulePolicies;
import com.uber.cadence.ScheduleSpec;
import java.util.List;

/**
 * A handle to an existing schedule. Obtain via {@link WorkflowClient#getScheduleHandle} or as the
 * return value of {@link WorkflowClient#createSchedule}.
 */
public interface ScheduleHandle {

  /** Schedule ID this handle refers to. */
  String getScheduleId();

  /** Returns the current full state and configuration of the schedule. */
  DescribeScheduleResponse describe() throws CadenceError;

  /**
   * Replaces the schedule configuration atomically. Any field not included is cleared by the server
   * — call {@link #describe} first to avoid inadvertently losing existing settings.
   */
  void update(ScheduleSpec spec, ScheduleAction action, SchedulePolicies policies)
      throws CadenceError;

  /** Permanently deletes this schedule. In-flight workflow runs are not affected. */
  void delete() throws CadenceError;

  /**
   * Pauses the schedule so no new runs are triggered.
   *
   * @param note reason stored with the pause, visible in {@link #describe}
   */
  void pause(String note) throws CadenceError;

  /**
   * Resumes a paused schedule.
   *
   * @param note reason stored with the unpause, visible in {@link #describe}
   */
  void unpause(String note) throws CadenceError;

  /**
   * Triggers runs for all times in the given historical ranges, subject to the configured overlap
   * policy. Excess runs may be skipped.
   */
  void backfill(List<BackfillInfo> backfills) throws CadenceError;
}
