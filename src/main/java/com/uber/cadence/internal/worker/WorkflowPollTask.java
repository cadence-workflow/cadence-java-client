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
  private final IWorkflowService service;
  private final String domain;
  private final String taskList;
  private final Semaphore decisionTaskExecutorSemaphore;
  private final PollForDecisionTaskRequest pollRequest;

  WorkflowPollTask(
      IWorkflowService service,
      String domain,
      String taskList,
      TaskListKind taskListKind,
      Scope metricScope,
      String identity,
      Semaphore decisionTaskExecutorSemaphore) {
    this.service = Objects.requireNonNull(service);
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.metricScope = Objects.requireNonNull(metricScope);
    this.decisionTaskExecutorSemaphore = Objects.requireNonNull(decisionTaskExecutorSemaphore);

    this.pollRequest = new PollForDecisionTaskRequest();
    pollRequest.setDomain(domain);
    pollRequest.setIdentity(identity);
    pollRequest.setBinaryChecksum(BinaryChecksum.getBinaryChecksum());
    TaskList tl = new TaskList().setName(taskList).setKind(taskListKind);
    pollRequest.setTaskList(tl);
  }

  @Override
  public DecisionTask poll() throws CadenceError {
    try {
      decisionTaskExecutorSemaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }

    boolean taskAcquired = false;
    try {
      metricScope.counter(MetricsType.DECISION_POLL_COUNTER).inc(1);
      MetricsEmit.DualStopwatch sw =
          MetricsEmit.startLatency(
              metricScope, MetricsType.DECISION_POLL_LATENCY, HistogramBuckets.DEFAULT_1MS_100S);

      if (log.isDebugEnabled()) {
        log.debug("poll request begin: " + pollRequest);
      }
      PollForDecisionTaskResponse result;
      try {
        log.info("polling for decision task");
        result = service.PollForDecisionTask(pollRequest);
      } catch (InternalServiceError e) {
        metricScope
            .tagged(ImmutableMap.of(MetricsTag.CAUSE, INTERNAL_SERVICE_ERROR))
            .counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER)
            .inc(1);
        throw e;
      } catch (ServiceBusyError e) {
        metricScope
            .tagged(ImmutableMap.of(MetricsTag.CAUSE, SERVICE_BUSY))
            .counter(MetricsType.DECISION_POLL_TRANSIENT_FAILED_COUNTER)
            .inc(1);
        throw e;
      } catch (CadenceError e) {
        metricScope.counter(MetricsType.DECISION_POLL_FAILED_COUNTER).inc(1);
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
        metricScope.counter(MetricsType.DECISION_POLL_NO_TASK_COUNTER).inc(1);
        return null;
      }

      Scope metricsScope =
          metricScope.tagged(
              ImmutableMap.of(MetricsTag.WORKFLOW_TYPE, result.getWorkflowType().getName()));
      metricsScope.counter(MetricsType.DECISION_POLL_SUCCEED_COUNTER).inc(1);
      Duration scheduledToStartLatency =
          Duration.ofNanos(result.getStartedTimestamp() - result.getScheduledTimestamp());
      MetricsEmit.emitLatency(
          metricsScope,
          MetricsType.DECISION_SCHEDULED_TO_START_LATENCY,
          scheduledToStartLatency,
          HistogramBuckets.DEFAULT_1MS_100S);
      sw.stop();
      taskAcquired = true;
      log.info(
          "task acquired: "
              + result.getWorkflowExecution()
              + " startEventId: "
              + result.getStartedEventId());
      return new DecisionTask(result, decisionTaskExecutorSemaphore::release);
    } finally {
      if (!taskAcquired) {
        log.info("releasing permits: " + decisionTaskExecutorSemaphore.availablePermits());
        decisionTaskExecutorSemaphore.release();
      }
    }
  }
}
