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
import com.uber.cadence.Memo;
import com.uber.cadence.PauseScheduleRequest;
import com.uber.cadence.PauseScheduleResponse;
import com.uber.cadence.RetryPolicy;
import com.uber.cadence.ScheduleStartWorkflowAction;
import com.uber.cadence.SearchAttributes;
import com.uber.cadence.TaskList;
import com.uber.cadence.UnpauseScheduleRequest;
import com.uber.cadence.UnpauseScheduleResponse;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.UpdateScheduleResponse;
import com.uber.cadence.WorkflowType;
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
  public CompletableFuture<CreateScheduleResponse> createSchedule(
      String scheduleId, ScheduleSpec spec, ScheduleAction action, SchedulePolicies policies) {
    try {
      CreateScheduleRequest request =
          new CreateScheduleRequest()
              .setSpec(toThriftSpec(spec))
              .setAction(toThriftAction(action))
              .setPolicies(toThriftPolicies(policies));
      return createSchedule(scheduleId, request);
    } catch (Exception e) {
      CompletableFuture<CreateScheduleResponse> f = new CompletableFuture<>();
      f.completeExceptionally(e);
      return f;
    }
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
  public CompletableFuture<UpdateScheduleResponse> updateSchedule(
      String scheduleId, ScheduleSpec spec, ScheduleAction action, SchedulePolicies policies) {
    try {
      UpdateScheduleRequest request =
          new UpdateScheduleRequest()
              .setSpec(toThriftSpec(spec))
              .setAction(toThriftAction(action))
              .setPolicies(toThriftPolicies(policies));
      return updateSchedule(scheduleId, request);
    } catch (Exception e) {
      CompletableFuture<UpdateScheduleResponse> f = new CompletableFuture<>();
      f.completeExceptionally(e);
      return f;
    }
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
    return unpauseSchedule(scheduleId, reason, null);
  }

  @Override
  public CompletableFuture<UnpauseScheduleResponse> unpauseSchedule(
      String scheduleId, String reason, ScheduleCatchUpPolicy catchUpPolicy) {
    UnpauseScheduleRequest request =
        new UnpauseScheduleRequest().setDomain(domain).setScheduleId(scheduleId).setReason(reason);
    if (catchUpPolicy != null) {
      request.setCatchUpPolicy(toThriftCatchUpPolicy(catchUpPolicy));
    }
    return service.UnpauseSchedule(request);
  }

  @Override
  public CompletableFuture<List<BackfillScheduleResponse>> backfillSchedule(
      String scheduleId, List<ScheduleBackfill> backfills) {
    // Requests are dispatched concurrently; the server handles ordering and overlap-policy
    // per-backfill. Results are collected in submission order.
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

  private static com.uber.cadence.ScheduleSpec toThriftSpec(ScheduleSpec s) {
    if (s == null) return null;
    com.uber.cadence.ScheduleSpec t =
        new com.uber.cadence.ScheduleSpec().setCronExpression(s.getCronExpression());
    if (s.getStartTime() != null) {
      Instant si = s.getStartTime();
      t.setStartTimeNano(si.getEpochSecond() * 1_000_000_000L + si.getNano());
    }
    if (s.getEndTime() != null) {
      Instant ei = s.getEndTime();
      t.setEndTimeNano(ei.getEpochSecond() * 1_000_000_000L + ei.getNano());
    }
    if (s.getJitter() != null) {
      t.setJitterInSeconds((int) s.getJitter().getSeconds());
    }
    return t;
  }

  private static com.uber.cadence.ScheduleAction toThriftAction(ScheduleAction a) {
    if (a == null) return null;
    return new com.uber.cadence.ScheduleAction()
        .setStartWorkflow(toThriftStartWorkflow(a.getStartWorkflow()));
  }

  private static ScheduleStartWorkflowAction toThriftStartWorkflow(
      ScheduleAction.StartWorkflowAction sw) {
    if (sw == null) return null;
    ScheduleStartWorkflowAction t = new ScheduleStartWorkflowAction();
    if (sw.getWorkflowType() != null) {
      t.setWorkflowType(new WorkflowType().setName(sw.getWorkflowType()));
    }
    if (sw.getTaskList() != null) {
      t.setTaskList(new TaskList().setName(sw.getTaskList()));
    }
    t.setInput(sw.getInput());
    t.setWorkflowIdPrefix(sw.getWorkflowIdPrefix());
    if (sw.getExecutionStartToCloseTimeout() != null) {
      t.setExecutionStartToCloseTimeoutSeconds(
          (int) sw.getExecutionStartToCloseTimeout().getSeconds());
    }
    if (sw.getTaskStartToCloseTimeout() != null) {
      t.setTaskStartToCloseTimeoutSeconds((int) sw.getTaskStartToCloseTimeout().getSeconds());
    }
    if (sw.getRetryOptions() != null) {
      t.setRetryPolicy(toThriftRetryPolicy(sw.getRetryOptions()));
    }
    if (sw.getMemo() != null && !sw.getMemo().isEmpty()) {
      Map<String, byte[]> fields = new HashMap<>();
      sw.getMemo()
          .forEach(
              (k, v) -> {
                if (!(v instanceof byte[])) {
                  throw new IllegalArgumentException(
                      "memo value for key '"
                          + k
                          + "' must be byte[], got "
                          + (v == null ? "null" : v.getClass().getName()));
                }
                fields.put(k, (byte[]) v);
              });
      t.setMemo(new Memo().setFields(fields));
    }
    if (sw.getSearchAttributes() != null && !sw.getSearchAttributes().isEmpty()) {
      Map<String, byte[]> indexedFields = new HashMap<>();
      sw.getSearchAttributes()
          .forEach(
              (k, v) -> {
                if (!(v instanceof byte[])) {
                  throw new IllegalArgumentException(
                      "searchAttributes value for key '"
                          + k
                          + "' must be byte[], got "
                          + (v == null ? "null" : v.getClass().getName()));
                }
                indexedFields.put(k, (byte[]) v);
              });
      t.setSearchAttributes(new SearchAttributes().setIndexedFields(indexedFields));
    }
    return t;
  }

  private static com.uber.cadence.SchedulePolicies toThriftPolicies(SchedulePolicies p) {
    if (p == null) return null;
    com.uber.cadence.SchedulePolicies t =
        new com.uber.cadence.SchedulePolicies()
            .setPauseOnFailure(p.isPauseOnFailure())
            .setBufferLimit(p.getBufferLimit())
            .setConcurrencyLimit(p.getConcurrencyLimit());
    if (p.getOverlapPolicy() != null) {
      t.setOverlapPolicy(toThriftOverlapPolicy(p.getOverlapPolicy()));
    }
    if (p.getCatchUpPolicy() != null) {
      t.setCatchUpPolicy(toThriftCatchUpPolicy(p.getCatchUpPolicy()));
    }
    if (p.getCatchUpWindow() != null) {
      t.setCatchUpWindowInSeconds((int) p.getCatchUpWindow().getSeconds());
    }
    return t;
  }

  private static com.uber.cadence.ScheduleCatchUpPolicy toThriftCatchUpPolicy(
      ScheduleCatchUpPolicy p) {
    switch (p) {
      case SKIP:
        return com.uber.cadence.ScheduleCatchUpPolicy.SKIP;
      case ONE:
        return com.uber.cadence.ScheduleCatchUpPolicy.ONE;
      case ALL:
        return com.uber.cadence.ScheduleCatchUpPolicy.ALL;
      default:
        throw new IllegalArgumentException("unknown ScheduleCatchUpPolicy: " + p);
    }
  }

  private static RetryPolicy toThriftRetryPolicy(RetryOptions r) {
    RetryPolicy t = new RetryPolicy().setBackoffCoefficient(r.getBackoffCoefficient());
    if (r.getInitialInterval() != null) {
      t.setInitialIntervalInSeconds((int) r.getInitialInterval().getSeconds());
    }
    if (r.getMaximumInterval() != null) {
      t.setMaximumIntervalInSeconds((int) r.getMaximumInterval().getSeconds());
    }
    if (r.getExpiration() != null) {
      t.setExpirationIntervalInSeconds((int) r.getExpiration().getSeconds());
    }
    t.setMaximumAttempts(r.getMaximumAttempts());
    return t;
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
