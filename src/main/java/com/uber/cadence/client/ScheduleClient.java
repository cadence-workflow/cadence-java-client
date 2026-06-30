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

import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;

/**
 * Client for schedule operations. Obtain via {@link WorkflowClient#scheduleClient()}.
 *
 * <pre>{@code
 * ScheduleClient sc = workflowClient.scheduleClient();
 * ScheduleHandle handle = sc.createSchedule("my-schedule", request);
 * handle.pause("maintenance window");
 * handle.delete();
 * }</pre>
 */
public interface ScheduleClient {

  /**
   * Creates a new schedule and returns a handle to it.
   *
   * @param scheduleId unique identifier for the schedule within the domain
   * @param request schedule configuration
   * @return a handle that can be used to manage the schedule
   */
  ScheduleHandle createSchedule(String scheduleId, CreateScheduleRequest request);

  /**
   * Returns a handle to an existing schedule. No server call is made; use {@link
   * ScheduleHandle#describe()} to verify the schedule exists.
   *
   * @param scheduleId the schedule identifier
   */
  ScheduleHandle getScheduleHandle(String scheduleId);

  /**
   * Lists schedules in the domain, paginated.
   *
   * @param request page size and continuation token
   */
  ListSchedulesResponse listSchedules(ListSchedulesRequest request);
}
