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

package com.uber.cadence.workflow;

import static org.junit.Assert.assertEquals;

import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityExecutionContext;
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.testUtils.CadenceTestRule;
import com.uber.cadence.worker.WorkerOptions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class LocalAsyncCompletionWorkflowTest {

  public static final int MAX_CONCURRENT_ACTIVITIES = 1;

  @Rule
  public CadenceTestRule testWorkflowRule =
      CadenceTestRule.builder()
          .withWorkerOptions(
              WorkerOptions.newBuilder()
                  .setMaxConcurrentActivityExecutionSize(MAX_CONCURRENT_ACTIVITIES)
                  .setActivityPollerOptions(
                      com.uber.cadence.internal.worker.PollerOptions.newBuilder()
                          .setPollThreadCount(5)
                          .build())
                  .build())
          .withWorkflowTypes(TestWorkflowImpl.class)
          .withActivities(new AsyncActivityWithManualCompletion())
          .startWorkersAutomatically()
          .build();

  public interface TestWorkflow {
    @WorkflowMethod
    String execute(String taskQueue);
  }

  public static class TestWorkflowImpl implements TestWorkflow {

    @Override
    public String execute(String taskQueue) {
      TestActivity activity =
          Workflow.newActivityStub(
              TestActivity.class,
              new ActivityOptions.Builder()
                  .setScheduleToStartTimeout(Duration.ofSeconds(10))
                  .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                  .setHeartbeatTimeout(Duration.ofSeconds(1))
                  .setRetryOptions(
                      new RetryOptions.Builder()
                          .setMaximumAttempts(1)
                          .setInitialInterval(Duration.ofSeconds(1))
                          .build())
                  .build());
      List<Promise<Integer>> promises = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        promises.add(Async.function(() -> activity.execute(2)));
      }
      Promise.allOf(promises).get();
      for (Promise<Integer> promise : promises) {
        if (promise.getFailure() != null) {
          return "exception";
        }
        if (promise.get() != 4) {
          return "wrong result";
        }
      }
      return "success";
    }
  }

  public interface TestActivity {
    @ActivityMethod
    int execute(int value);
  }

  public static class AsyncActivityWithManualCompletion implements TestActivity {
    private final AtomicInteger concurrentActivitiesCount = new AtomicInteger(0);

    @Override
    public int execute(int value) {
      int concurrentActivities = concurrentActivitiesCount.incrementAndGet();
      if (concurrentActivities > MAX_CONCURRENT_ACTIVITIES) {
        throw new RuntimeException(
            String.format(
                "More than %d activities was running concurrently!", MAX_CONCURRENT_ACTIVITIES));
      }
      ActivityExecutionContext context = Activity.getExecutionContext();
      Activity.heartbeat(value);
      ActivityCompletionClient completionClient = context.useLocalManualCompletion();
      ForkJoinPool.commonPool().execute(() -> asyncActivityFn(value, context, completionClient));
      return 0;
    }

    private void asyncActivityFn(
        int value, ActivityExecutionContext context, ActivityCompletionClient completionClient) {
      try {
        Thread.sleep(500);
        concurrentActivitiesCount.decrementAndGet();
        completionClient.complete(context.getTaskToken(), value * 2);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        e.printStackTrace();
        concurrentActivitiesCount.decrementAndGet();
        completionClient.completeExceptionally(context.getTaskToken(), e);
      }
    }
  }

  @Test
  public void verifyLocalActivityCompletionRespectsConcurrencySettings() {
    String taskQueue = testWorkflowRule.getTaskList();
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    TestWorkflow workflow =
        workflowClient.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setTaskList(taskQueue)
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
                .build());
    String result = workflow.execute(taskQueue);
    assertEquals("success", result);
  }
}
