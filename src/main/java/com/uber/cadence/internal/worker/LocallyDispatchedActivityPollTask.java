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

import com.uber.cadence.CadenceError;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.worker.LocallyDispatchedActivityWorker.Task;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocallyDispatchedActivityPollTask extends ActivityPollTaskBase
    implements Function<Task, Boolean> {

  private static final Logger log =
      LoggerFactory.getLogger(LocallyDispatchedActivityPollTask.class);
  private final SynchronousQueue<Task> pendingTasks = new SynchronousQueue<>();

  public LocallyDispatchedActivityPollTask(SingleWorkerOptions options) {
    super(options);
  }

  @Override
  protected PollForActivityTaskResponse pollTask() throws CadenceError {
    Task task;
    try {
      task = pendingTasks.take();
    } catch (InterruptedException e) {
      throw new RuntimeException("locally dispatch activity poll task interrupted", e);
    }
    try {
      if (!task.await()) {
        options
            .getMetricsScope()
            .counter(MetricsType.LOCALLY_DISPATCHED_ACTIVITY_POLL_NO_TASK_COUNTER)
            .inc(1);
        return null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("locally dispatch activity await task interrupted", e);
    }
    options.getMetricsScope().counter(MetricsType.ACTIVITY_POLL_COUNTER).inc(1);
    options
        .getMetricsScope()
        .counter(MetricsType.LOCALLY_DISPATCHED_ACTIVITY_POLL_SUCCEED_COUNTER)
        .inc(1);
    PollForActivityTaskResponse result =
        new PollForActivityTaskResponse()
            .setActivityId(task.activityId)
            .setActivityType(task.activityType)
            .setHeader(task.header)
            .setInput(task.input.array())
            .setWorkflowExecution(task.workflowExecution)
            .setScheduledTimestampOfThisAttempt(task.scheduledTimestampOfThisAttempt)
            .setScheduledTimestamp(task.scheduledTimestamp)
            .setScheduleToCloseTimeoutSeconds(task.scheduleToCloseTimeoutSeconds)
            .setStartedTimestamp(task.startedTimestamp)
            .setStartToCloseTimeoutSeconds(task.startToCloseTimeoutSeconds)
            .setHeartbeatTimeoutSeconds(task.heartbeatTimeoutSeconds)
            .setTaskToken(task.taskToken.array())
            .setWorkflowType(task.workflowType)
            .setWorkflowDomain(task.workflowDomain)
            .setAttempt(0);
    return result;
  }

  @Override
  public Boolean apply(Task task) {
    // non blocking put to the unbuffered queue
    return pendingTasks.offer(task);
  }
}
