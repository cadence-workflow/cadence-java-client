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

import com.uber.cadence.ActivityType;
import com.uber.cadence.Header;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.worker.Poller.PollTask;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public final class LocallyDispatchedActivityWorker extends ActivityWorker {

  private LocallyDispatchedActivityPollTask ldaPollTask;

  public LocallyDispatchedActivityWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler) {
    super(
        service,
        domain,
        taskList,
        options,
        handler,
        "Locally Dispatched Activity Poller taskList=");
    ldaPollTask = new LocallyDispatchedActivityPollTask(options);
  }

  protected PollTask<ActivityTask> getOrCreateActivityPollTask() {
    return ldaPollTask;
  }

  public Function<Task, Boolean> getLocallyDispatchedActivityTaskPoller() {
    return ldaPollTask;
  }

  public static class Task {

    protected final WorkflowExecution workflowExecution;
    protected final String activityId;
    protected final ActivityType activityType;
    protected final ByteBuffer input;
    protected final int scheduleToCloseTimeoutSeconds;
    protected final int startToCloseTimeoutSeconds;
    protected final int heartbeatTimeoutSeconds;
    protected final WorkflowType workflowType;
    protected final String workflowDomain;
    protected final Header header;
    // used to notify the poller the response from server is completed and the task is ready
    private final CountDownLatch latch = new CountDownLatch(1);
    protected long scheduledTimestamp;
    protected long scheduledTimestampOfThisAttempt;
    protected long startedTimestamp;
    protected ByteBuffer taskToken;
    private volatile boolean ready;

    public Task(
        String activityId,
        ActivityType activityType,
        ByteBuffer input,
        int scheduleToCloseTimeoutSeconds,
        int startToCloseTimeoutSeconds,
        int heartbeatTimeoutSeconds,
        WorkflowType workflowType,
        String workflowDomain,
        Header header,
        WorkflowExecution workflowExecution) {
      this.workflowExecution = workflowExecution;
      this.activityId = activityId;
      this.activityType = activityType;
      this.input = input;
      this.scheduleToCloseTimeoutSeconds = scheduleToCloseTimeoutSeconds;
      this.startToCloseTimeoutSeconds = startToCloseTimeoutSeconds;
      this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
      this.workflowType = workflowType;
      this.workflowDomain = workflowDomain;
      this.header = header;
    }

    protected boolean await() throws InterruptedException {
      latch.await();
      return ready;
    }

    public void notify(boolean ready) {
      this.ready = ready;
      latch.countDown();
    }
  }
}
