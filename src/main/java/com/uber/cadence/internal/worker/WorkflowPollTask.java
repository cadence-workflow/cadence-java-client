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

package com.uber.cadence.internal.worker;

import static com.uber.cadence.internal.metrics.MetricsTagValue.INTERNAL_SERVICE_ERROR;
import static com.uber.cadence.internal.metrics.MetricsTagValue.SERVICE_BUSY;

import com.uber.cadence.*;
import com.uber.cadence.common.BinaryChecksum;
import com.uber.cadence.internal.metrics.HistogramBuckets;
import com.uber.cadence.internal.metrics.MetricsEmit;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkflowPollTask implements Poller.PollTask<DecisionTask> {

  private static final Logger log = LoggerFactory.getLogger(WorkflowWorker.class);
  private final Scope metricScope;
  private final Scope stickyMetricScope;
  private final IWorkflowService service;
  private final String domain;
  private final String taskList;
  private final Semaphore decisionTaskExecutorSemaphore;
  private final StickyQueueBalancer stickyQueueBalancer;
  private final PollForDecisionTaskRequest pollRequest;
  private final PollForDecisionTaskRequest stickyPollRequest;

  WorkflowPollTask(
      IWorkflowService service,
      String domain,
      String taskList,
      String stickyTaskListName,
      Scope metricScope,
      String identity,
      Semaphore decisionTaskExecutorSemaphore,
      StickyQueueBalancer stickyQueueBalancer) {
    this.service = Objects.requireNonNull(service);
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.metricScope = Objects.requireNonNull(metricScope);
    this.stickyQueueBalancer = Objects.requireNonNull(stickyQueueBalancer);
    this.decisionTaskExecutorSemaphore = Objects.requireNonNull(decisionTaskExecutorSemaphore);

    this.stickyMetricScope =
        metricScope.tagged(
            new ImmutableMap.Builder<String, String>(1)
                .put(MetricsTag.TASK_LIST, String.format("%s:%s", taskList, "sticky"))
                .build());

    // Normal poll request
    this.pollRequest = new PollForDecisionTaskRequest();
    pollRequest.setDomain(domain);
    pollRequest.setIdentity(identity);
    pollRequest.setBinaryChecksum(BinaryChecksum.getBinaryChecksum());
    TaskList tl = new TaskList().setName(taskList).setKind(TaskListKind.NORMAL);
    pollRequest.setTaskList(tl);

    // Sticky poll request
    this.stickyPollRequest = new PollForDecisionTaskRequest();
    stickyPollRequest.setDomain(domain);
    stickyPollRequest.setIdentity(identity);
    stickyPollRequest.setBinaryChecksum(BinaryChecksum.getBinaryChecksum());
    TaskList stickyTl = new TaskList().setName(stickyTaskListName).setKind(TaskListKind.STICKY);
    stickyPollRequest.setTaskList(stickyTl);
  }

  @Override
  public DecisionTask poll() throws CadenceError {
    boolean isSuccessful = false;
    try {
      decisionTaskExecutorSemaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }

    TaskListKind taskListKind = stickyQueueBalancer.makePoll();
    boolean isSticky = TaskListKind.STICKY.equals(taskListKind);
    PollForDecisionTaskRequest request = isSticky ? stickyPollRequest : pollRequest;
    Scope scope = isSticky ? stickyMetricScope : metricScope;

    log.trace("poll request begin: {}", request);
    try {
      PollForDecisionTaskResponse response = doPoll(request, scope);
      if (response == null) {
        return null;
      }
      isSuccessful = true;
      stickyQueueBalancer.finishPoll(taskListKind, response.getBacklogCountHint());
      log.info(
          "task acquired: "
              + response.getWorkflowExecution()
              + " startEventId: "
              + response.getStartedEventId());
      return new DecisionTask(response, decisionTaskExecutorSemaphore::release);
    } finally {
      if (!isSuccessful) {
        log.info("releasing permits: " + decisionTaskExecutorSemaphore.availablePermits());
        decisionTaskExecutorSemaphore.release();
        stickyQueueBalancer.finishPoll(taskListKind);
      }
    }
  }

  private PollForDecisionTaskResponse doPoll(PollForDecisionTaskRequest request, Scope scope)
      throws CadenceError {
    scope.counter(MetricsType.DECISION_POLL_COUNTER).inc(1);
    MetricsEmit.DualStopwatch sw =
        MetricsEmit.startLatency(
            scope, MetricsType.DECISION_POLL_LATENCY, HistogramBuckets.DEFAULT_1MS_100S);

    if (log.isDebugEnabled()) {
      log.debug("poll request begin: " + request);
    }
    PollForDecisionTaskResponse result;
    try {
      log.info("polling for decision task: " + request);
      result = service.PollForDecisionTask(request);
    } catch (InternalServiceError e) {
      scope
          .tagged(ImmutableMap.of(MetricsTag.CAUSE, INTERNAL_SERVICE_ERROR))
          .counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER)
          .inc(1);
      throw e;
    } catch (ServiceBusyError e) {
      scope
          .tagged(ImmutableMap.of(MetricsTag.CAUSE, SERVICE_BUSY))
          .counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER)
          .inc(1);
      throw e;
    } catch (CadenceError e) {
      scope.counter(MetricsType.DECISION_POLL_FAILED_COUNTER).inc(1);
      throw e;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "poll request returned decision task: workflowType="
              + result.getWorkflowType()
              + ", workflowExecution="
              + result.getWorkflowExecution()
              + ", startedEventId="
              + result.getStartedEventId()
              + ", previousStartedEventId="
              + result.getPreviousStartedEventId()
              + (result.getQuery() != null
                  ? ", queryType=" + result.getQuery().getQueryType()
                  : ""));
    }

    if (result == null || result.getTaskToken() == null) {
      scope.counter(MetricsType.DECISION_POLL_NO_TASK_COUNTER).inc(1);
      return null;
    }

    Scope metricsScope =
        scope.tagged(ImmutableMap.of(MetricsTag.WORKFLOW_TYPE, result.getWorkflowType().getName()));
    metricsScope.counter(MetricsType.DECISION_POLL_SUCCEED_COUNTER).inc(1);
    Duration scheduledToStartLatency =
        Duration.ofNanos(result.getStartedTimestamp() - result.getScheduledTimestamp());
    MetricsEmit.emitLatency(
        metricsScope,
        MetricsType.DECISION_SCHEDULED_TO_START_LATENCY,
        scheduledToStartLatency,
        HistogramBuckets.DEFAULT_1MS_100S);
    sw.stop();
    return result;
  }
}
