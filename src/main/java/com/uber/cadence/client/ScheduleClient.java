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
import com.uber.cadence.CreateScheduleResponse;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.UpdateScheduleRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client for managing schedules within a domain. Obtain via {@link
 * WorkflowClient#scheduleClient()}.
 *
 * <p>All methods return {@link CompletableFuture}. Synchronous callers can block with {@link
 * CompletableFuture#get()} or {@link CompletableFuture#join()}.
 *
 * <pre>{@code
 * ScheduleClient sc = workflowClient.scheduleClient();
 * sc.createSchedule("my-schedule", request).join();
 * sc.pauseSchedule("my-schedule", "maintenance window").join();
 * sc.deleteSchedule("my-schedule").join();
 * }</pre>
 */
public interface ScheduleClient {

  /**
   * Creates a new schedule. The {@code domain} and {@code scheduleId} fields of the request are set
   * automatically; any value provided for them is ignored.
   *
   * @param scheduleId unique identifier for the schedule within the domain
   * @param request schedule configuration (spec, action, policies, etc.)
   */
  CompletableFuture<CreateScheduleResponse> createSchedule(
      String scheduleId, CreateScheduleRequest request);

  /**
   * Returns the current configuration and runtime state of a schedule.
   *
   * @param scheduleId the schedule identifier
   */
  CompletableFuture<DescribeScheduleResponse> describeSchedule(String scheduleId);

  /**
   * Replaces the configuration of an existing schedule. The {@code domain} and {@code scheduleId}
   * fields of the request are set automatically. Any field not included in the request is cleared
   * by the server; call {@link #describeSchedule} first to avoid losing existing settings.
   *
   * @param scheduleId the schedule identifier
   * @param request new configuration (spec, action, policies, etc.)
   */
  CompletableFuture<Void> updateSchedule(String scheduleId, UpdateScheduleRequest request);

  /**
   * Permanently deletes a schedule. In-flight workflow runs triggered by this schedule are not
   * affected.
   *
   * @param scheduleId the schedule identifier
   */
  CompletableFuture<Void> deleteSchedule(String scheduleId);

  /**
   * Pauses a schedule so no new runs are triggered.
   *
   * @param scheduleId the schedule identifier
   * @param reason stored as the pause note, visible in {@link #describeSchedule}
   */
  CompletableFuture<Void> pauseSchedule(String scheduleId, String reason);

  /**
   * Resumes a paused schedule.
   *
   * @param scheduleId the schedule identifier
   * @param reason stored as the unpause note, visible in {@link #describeSchedule}
   */
  CompletableFuture<Void> unpauseSchedule(String scheduleId, String reason);

  /**
   * Triggers runs for all times in the given historical ranges. One service call is made per entry.
   *
   * @param scheduleId the schedule identifier
   * @param backfills time ranges to backfill
   */
  CompletableFuture<Void> backfillSchedule(String scheduleId, List<ScheduleBackfill> backfills);

  /**
   * Lists schedules in the domain, paginated. The {@code domain} field of the request is set
   * automatically.
   *
   * @param request page size and continuation token
   */
  CompletableFuture<ListSchedulesResponse> listSchedules(ListSchedulesRequest request);
}
