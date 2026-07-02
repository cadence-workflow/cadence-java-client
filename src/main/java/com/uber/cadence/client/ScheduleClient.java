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

import com.uber.cadence.BackfillScheduleResponse;
import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.CreateScheduleResponse;
import com.uber.cadence.DeleteScheduleResponse;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.PauseScheduleResponse;
import com.uber.cadence.UnpauseScheduleResponse;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.UpdateScheduleResponse;
import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import com.uber.cadence.client.schedule.ScheduleDescription;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
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
   * Creates a new schedule. The {@code domain} and {@code scheduleId} fields of the request are
   * overridden by the client; any value provided for them is ignored.
   *
   * @param scheduleId unique identifier for the schedule within the domain
   * @param request schedule configuration (spec, action, policies, etc.)
   */
  CompletableFuture<CreateScheduleResponse> createSchedule(
      String scheduleId, CreateScheduleRequest request);

  /**
   * Creates a new schedule using clean client types. Equivalent to constructing a {@link
   * com.uber.cadence.CreateScheduleRequest} manually and calling {@link #createSchedule(String,
   * com.uber.cadence.CreateScheduleRequest)}. Use the raw-request overload if you need to set memo
   * or search attributes.
   *
   * @param scheduleId unique identifier for the schedule within the domain
   * @param spec when and how often the schedule fires
   * @param action what to do on each firing (start a workflow)
   * @param policies overlap, catch-up, and failure-handling policies
   */
  CompletableFuture<CreateScheduleResponse> createSchedule(
      String scheduleId, ScheduleSpec spec, ScheduleAction action, SchedulePolicies policies);

  /**
   * Returns the current configuration and runtime state of a schedule.
   *
   * @param scheduleId the schedule identifier
   */
  CompletableFuture<ScheduleDescription> describeSchedule(String scheduleId);

  /**
   * Replaces the configuration of an existing schedule. The {@code domain} and {@code scheduleId}
   * fields of the request are overridden by the client. Any field not included in the request is
   * cleared by the server; call {@link #describeSchedule} first to avoid losing existing settings.
   *
   * @param scheduleId the schedule identifier
   * @param request new configuration (spec, action, policies, etc.)
   */
  CompletableFuture<UpdateScheduleResponse> updateSchedule(
      String scheduleId, UpdateScheduleRequest request);

  /**
   * Replaces the configuration of an existing schedule using clean client types. Equivalent to
   * constructing an {@link com.uber.cadence.UpdateScheduleRequest} manually and calling {@link
   * #updateSchedule(String, com.uber.cadence.UpdateScheduleRequest)}. Any field not included is
   * cleared by the server; call {@link #describeSchedule} first to avoid losing existing settings.
   *
   * @param scheduleId the schedule identifier
   * @param spec new schedule spec
   * @param action new workflow action
   * @param policies new overlap/catch-up/failure policies
   */
  CompletableFuture<UpdateScheduleResponse> updateSchedule(
      String scheduleId, ScheduleSpec spec, ScheduleAction action, SchedulePolicies policies);

  /**
   * Permanently deletes a schedule. In-flight workflow runs triggered by this schedule are not
   * affected.
   *
   * @param scheduleId the schedule identifier
   */
  CompletableFuture<DeleteScheduleResponse> deleteSchedule(String scheduleId);

  /**
   * Pauses a schedule so no new runs are triggered.
   *
   * @param scheduleId the schedule identifier
   * @param reason stored as the pause note, visible in {@link #describeSchedule}
   */
  CompletableFuture<PauseScheduleResponse> pauseSchedule(String scheduleId, String reason);

  /**
   * Resumes a paused schedule.
   *
   * @param scheduleId the schedule identifier
   * @param reason stored as the unpause note, visible in {@link #describeSchedule}
   */
  CompletableFuture<UnpauseScheduleResponse> unpauseSchedule(String scheduleId, String reason);

  /**
   * Resumes a paused schedule, overriding the catch-up policy for this unpause only. Use this when
   * you want different catch-up behavior than the schedule's configured default, e.g. skipping all
   * missed firings after a long pause.
   *
   * @param scheduleId the schedule identifier
   * @param reason stored as the unpause note, visible in {@link #describeSchedule}
   * @param catchUpPolicy catch-up policy to apply for missed firings on this unpause
   */
  CompletableFuture<UnpauseScheduleResponse> unpauseSchedule(
      String scheduleId, String reason, ScheduleCatchUpPolicy catchUpPolicy);

  /**
   * Triggers runs for all times in the given historical ranges. One service call is made per entry.
   *
   * @param scheduleId the schedule identifier
   * @param backfills time ranges to backfill
   */
  CompletableFuture<List<BackfillScheduleResponse>> backfillSchedule(
      String scheduleId, List<ScheduleBackfill> backfills);

  /**
   * Lists schedules in the domain, paginated.
   *
   * @param pageSize maximum number of schedules to return
   * @param nextPageToken continuation token from a previous response, or {@code null} for the first
   *     page
   */
  CompletableFuture<ListSchedulesResponse> listSchedules(int pageSize, byte[] nextPageToken);
}
