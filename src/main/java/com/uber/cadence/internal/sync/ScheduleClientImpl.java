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

package com.uber.cadence.internal.sync;

import com.uber.cadence.BackfillScheduleRequest;
import com.uber.cadence.BackfillScheduleResponse;
import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.CreateScheduleResponse;
import com.uber.cadence.DeleteScheduleRequest;
import com.uber.cadence.DeleteScheduleResponse;
import com.uber.cadence.DescribeScheduleRequest;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.PauseScheduleRequest;
import com.uber.cadence.PauseScheduleResponse;
import com.uber.cadence.UnpauseScheduleRequest;
import com.uber.cadence.UnpauseScheduleResponse;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.UpdateScheduleResponse;
import com.uber.cadence.client.ScheduleBackfill;
import com.uber.cadence.client.ScheduleClient;
import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import com.uber.cadence.client.schedule.ScheduleDescription;
import com.uber.cadence.client.schedule.ScheduleInfo;
import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import com.uber.cadence.client.schedule.ScheduleState;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class ScheduleClientImpl implements ScheduleClient {

  private final IWorkflowService service;
  private final String domain;

  ScheduleClientImpl(IWorkflowService service, String domain) {
    this.service = service;
    this.domain = domain;
  }

  @Override
  public CompletableFuture<CreateScheduleResponse> createSchedule(
      String scheduleId, CreateScheduleRequest request) {
    request.setDomain(domain);
    request.setScheduleId(scheduleId);
    return service.CreateSchedule(request);
  }

  @Override
  public CompletableFuture<ScheduleDescription> describeSchedule(String scheduleId) {
    DescribeScheduleRequest request =
        new DescribeScheduleRequest().setDomain(domain).setScheduleId(scheduleId);
    return service.DescribeSchedule(request).thenApply(ScheduleClientImpl::toScheduleDescription);
  }

  @Override
  public CompletableFuture<UpdateScheduleResponse> updateSchedule(
      String scheduleId, UpdateScheduleRequest request) {
    request.setDomain(domain);
    request.setScheduleId(scheduleId);
    return service.UpdateSchedule(request);
  }

  @Override
  public CompletableFuture<DeleteScheduleResponse> deleteSchedule(String scheduleId) {
    DeleteScheduleRequest request =
        new DeleteScheduleRequest().setDomain(domain).setScheduleId(scheduleId);
    return service.DeleteSchedule(request);
  }

  @Override
  public CompletableFuture<PauseScheduleResponse> pauseSchedule(String scheduleId, String reason) {
    PauseScheduleRequest request =
        new PauseScheduleRequest().setDomain(domain).setScheduleId(scheduleId).setReason(reason);
    return service.PauseSchedule(request);
  }

  @Override
  public CompletableFuture<UnpauseScheduleResponse> unpauseSchedule(
      String scheduleId, String reason) {
    UnpauseScheduleRequest request =
        new UnpauseScheduleRequest().setDomain(domain).setScheduleId(scheduleId).setReason(reason);
    return service.UnpauseSchedule(request);
  }

  @Override
  public CompletableFuture<List<BackfillScheduleResponse>> backfillSchedule(
      String scheduleId, List<ScheduleBackfill> backfills) {
    List<CompletableFuture<BackfillScheduleResponse>> futures = new ArrayList<>();
    for (ScheduleBackfill bf : backfills) {
      BackfillScheduleRequest request =
          new BackfillScheduleRequest()
              .setDomain(domain)
              .setScheduleId(scheduleId)
              .setStartTimeNano(bf.getStartTime().toEpochMilli() * 1_000_000L)
              .setEndTimeNano(bf.getEndTime().toEpochMilli() * 1_000_000L);
      if (bf.getOverlapPolicy() != null) {
        request.setOverlapPolicy(toThriftOverlapPolicy(bf.getOverlapPolicy()));
      }
      futures.add(service.BackfillSchedule(request));
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              List<BackfillScheduleResponse> results = new ArrayList<>();
              for (CompletableFuture<BackfillScheduleResponse> f : futures) {
                results.add(f.join());
              }
              return results;
            });
  }

  @Override
  public CompletableFuture<ListSchedulesResponse> listSchedules(
      int pageSize, byte[] nextPageToken) {
    ListSchedulesRequest request =
        new ListSchedulesRequest()
            .setDomain(domain)
            .setPageSize(pageSize)
            .setNextPageToken(nextPageToken);
    return service.ListSchedules(request);
  }

  private static ScheduleDescription toScheduleDescription(DescribeScheduleResponse r) {
    if (r == null) {
      return null;
    }
    return new ScheduleDescription(
        toScheduleSpec(r.getSpec()),
        toScheduleAction(r.getAction()),
        toSchedulePolicies(r.getPolicies()),
        toScheduleState(r.getState()),
        toScheduleInfo(r.getInfo()),
        toObjectMap(r.getMemo() != null ? r.getMemo().getFields() : null),
        toObjectMap(
            r.getSearchAttributes() != null ? r.getSearchAttributes().getIndexedFields() : null));
  }

  private static ScheduleSpec toScheduleSpec(com.uber.cadence.ScheduleSpec t) {
    if (t == null) return null;
    return ScheduleSpec.newBuilder()
        .setCronExpression(t.getCronExpression())
        .setStartTime(nanosToInstant(t.getStartTimeNano()))
        .setEndTime(nanosToInstant(t.getEndTimeNano()))
        .setJitter(Duration.ofSeconds(t.getJitterInSeconds()))
        .build();
  }

  private static ScheduleAction toScheduleAction(com.uber.cadence.ScheduleAction t) {
    if (t == null || t.getStartWorkflow() == null) return null;
    com.uber.cadence.ScheduleStartWorkflowAction sw = t.getStartWorkflow();
    ScheduleAction.StartWorkflowAction.Builder swb =
        ScheduleAction.StartWorkflowAction.newBuilder()
            .setWorkflowType(sw.getWorkflowType() != null ? sw.getWorkflowType().getName() : null)
            .setTaskList(sw.getTaskList() != null ? sw.getTaskList().getName() : null)
            .setInput(sw.getInput())
            .setWorkflowIdPrefix(sw.getWorkflowIdPrefix())
            .setExecutionStartToCloseTimeout(
                Duration.ofSeconds(sw.getExecutionStartToCloseTimeoutSeconds()))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(sw.getTaskStartToCloseTimeoutSeconds()));
    if (sw.getRetryPolicy() != null) {
      swb.setRetryOptions(toRetryOptions(sw.getRetryPolicy()));
    }
    if (sw.getMemo() != null) {
      swb.setMemo(toObjectMap(sw.getMemo().getFields()));
    }
    if (sw.getSearchAttributes() != null) {
      swb.setSearchAttributes(toObjectMap(sw.getSearchAttributes().getIndexedFields()));
    }
    return ScheduleAction.newBuilder().setStartWorkflow(swb.build()).build();
  }

  private static SchedulePolicies toSchedulePolicies(com.uber.cadence.SchedulePolicies t) {
    if (t == null) return null;
    return SchedulePolicies.newBuilder()
        .setOverlapPolicy(toClientOverlapPolicy(t.getOverlapPolicy()))
        .setCatchUpPolicy(toClientCatchUpPolicy(t.getCatchUpPolicy()))
        .setCatchUpWindow(Duration.ofSeconds(t.getCatchUpWindowInSeconds()))
        .setPauseOnFailure(t.isPauseOnFailure())
        .setBufferLimit(t.getBufferLimit())
        .setConcurrencyLimit(t.getConcurrencyLimit())
        .build();
  }

  private static ScheduleState toScheduleState(com.uber.cadence.ScheduleState t) {
    if (t == null) return null;
    String pauseReason = null;
    Instant pausedAt = null;
    String pausedBy = null;
    if (t.getPauseInfo() != null) {
      com.uber.cadence.SchedulePauseInfo pi = t.getPauseInfo();
      pauseReason = pi.getReason();
      pausedAt = nanosToInstant(pi.getPausedTimeNano());
      pausedBy = pi.getPausedBy();
    }
    return new ScheduleState(t.isPaused(), pauseReason, pausedAt, pausedBy);
  }

  private static ScheduleInfo toScheduleInfo(com.uber.cadence.ScheduleInfo t) {
    if (t == null) return null;
    List<ScheduleInfo.BackfillInfo> backfills = new ArrayList<>();
    if (t.getOngoingBackfills() != null) {
      for (com.uber.cadence.BackfillInfo b : t.getOngoingBackfills()) {
        backfills.add(
            new ScheduleInfo.BackfillInfo(
                b.getBackfillId(),
                nanosToInstant(b.getStartTimeNano()),
                nanosToInstant(b.getEndTimeNano()),
                b.getRunsCompleted(),
                b.getRunsTotal()));
      }
    }
    return new ScheduleInfo(
        nanosToInstant(t.getLastRunTimeNano()),
        nanosToInstant(t.getNextRunTimeNano()),
        t.getTotalRuns(),
        nanosToInstant(t.getCreateTimeNano()),
        nanosToInstant(t.getLastUpdateTimeNano()),
        backfills,
        t.getMissedRuns(),
        t.getSkippedRuns());
  }

  private static RetryOptions toRetryOptions(com.uber.cadence.RetryPolicy p) {
    return new RetryOptions.Builder()
        .setInitialInterval(Duration.ofSeconds(p.getInitialIntervalInSeconds()))
        .setMaximumInterval(Duration.ofSeconds(p.getMaximumIntervalInSeconds()))
        .setBackoffCoefficient(p.getBackoffCoefficient())
        .setMaximumAttempts(p.getMaximumAttempts())
        .setExpiration(Duration.ofSeconds(p.getExpirationIntervalInSeconds()))
        .build();
  }

  private static ScheduleOverlapPolicy toClientOverlapPolicy(
      com.uber.cadence.ScheduleOverlapPolicy t) {
    if (t == null) return null;
    switch (t) {
      case SKIP_NEW:
        return ScheduleOverlapPolicy.SKIP_NEW;
      case BUFFER:
        return ScheduleOverlapPolicy.BUFFER;
      case CONCURRENT:
        return ScheduleOverlapPolicy.CONCURRENT;
      case CANCEL_PREVIOUS:
        return ScheduleOverlapPolicy.CANCEL_PREVIOUS;
      case TERMINATE_PREVIOUS:
        return ScheduleOverlapPolicy.TERMINATE_PREVIOUS;
      default:
        return null;
    }
  }

  private static com.uber.cadence.ScheduleOverlapPolicy toThriftOverlapPolicy(
      ScheduleOverlapPolicy p) {
    switch (p) {
      case SKIP_NEW:
        return com.uber.cadence.ScheduleOverlapPolicy.SKIP_NEW;
      case BUFFER:
        return com.uber.cadence.ScheduleOverlapPolicy.BUFFER;
      case CONCURRENT:
        return com.uber.cadence.ScheduleOverlapPolicy.CONCURRENT;
      case CANCEL_PREVIOUS:
        return com.uber.cadence.ScheduleOverlapPolicy.CANCEL_PREVIOUS;
      case TERMINATE_PREVIOUS:
        return com.uber.cadence.ScheduleOverlapPolicy.TERMINATE_PREVIOUS;
      default:
        throw new IllegalArgumentException("unknown ScheduleOverlapPolicy: " + p);
    }
  }

  private static ScheduleCatchUpPolicy toClientCatchUpPolicy(
      com.uber.cadence.ScheduleCatchUpPolicy t) {
    if (t == null) return null;
    switch (t) {
      case SKIP:
        return ScheduleCatchUpPolicy.SKIP;
      case ONE:
        return ScheduleCatchUpPolicy.ONE;
      case ALL:
        return ScheduleCatchUpPolicy.ALL;
      default:
        return null;
    }
  }

  private static Instant nanosToInstant(long nanos) {
    if (nanos == 0) return null;
    return Instant.ofEpochSecond(nanos / 1_000_000_000L, nanos % 1_000_000_000L);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> toObjectMap(Map<String, ?> src) {
    if (src == null || src.isEmpty()) return null;
    Map<String, Object> result = new HashMap<>();
    src.forEach((k, v) -> result.put(k, v));
    return result;
  }
}
