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

import com.uber.cadence.InternalServiceError;
import com.uber.cadence.PollForDecisionTaskRequest;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.TaskList;
import com.uber.cadence.TaskListKind;
import com.uber.cadence.common.BinaryChecksum;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;
import java.util.Objects;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkflowPollTask implements Poller.PollTask<PollForDecisionTaskResponse> {

  private static final Logger log = LoggerFactory.getLogger(WorkflowWorker.class);
  private final Scope metricScope;
  private final IWorkflowService service;
  private final String domain;
  private final String taskList;
  private final TaskListKind taskListKind;
  private final String identity;

  WorkflowPollTask(
      IWorkflowService service,
      String domain,
      String taskList,
      TaskListKind taskListKind,
      Scope metricScope,
      String identity) {
    this.identity = Objects.requireNonNull(identity);
    this.service = Objects.requireNonNull(service);
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.taskListKind = Objects.requireNonNull(taskListKind);
    this.metricScope = Objects.requireNonNull(metricScope);
  }

  @Override
  public PollForDecisionTaskResponse poll() throws TException {
    metricScope.counter(MetricsType.DECISION_POLL_COUNTER).inc(1);
    Stopwatch sw = metricScope.timer(MetricsType.DECISION_POLL_LATENCY).start();

    PollForDecisionTaskRequest pollRequest = new PollForDecisionTaskRequest();
    pollRequest.setDomain(domain);
    pollRequest.setIdentity(identity);
    pollRequest.setBinaryChecksum(BinaryChecksum.getBinaryChecksum());

    TaskList tl = new TaskList().setName(taskList).setKind(taskListKind);
    pollRequest.setTaskList(tl);

    if (log.isDebugEnabled()) {
      log.debug("poll request begin: " + pollRequest);
    }
    PollForDecisionTaskResponse result;
    try {
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
    } catch (TException e) {
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
    metricsScope
        .timer(MetricsType.DECISION_SCHEDULED_TO_START_LATENCY)
        .record(Duration.ofNanos(result.getStartedTimestamp() - result.getScheduledTimestamp()));
    sw.stop();
    return result;
  }
}
