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
import com.uber.cadence.activity.ActivityMethod;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.testUtils.CadenceTestRule;
import com.uber.cadence.worker.WorkerOptions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class ActivityPollerPrefetchingTest {

  @Rule
  public CadenceTestRule testWorkflowRule =
      CadenceTestRule.builder()
          .withWorkerOptions(
              WorkerOptions.newBuilder()
                  .setMaxConcurrentActivityExecutionSize(1)
                  .setActivityPollerOptions(
                      com.uber.cadence.internal.worker.PollerOptions.newBuilder()
                          .setPollThreadCount(5)
                          .build())
                  .build())
          .withWorkflowTypes(TestWorkflowImpl.class)
          .withActivities(new SleepyMultiplier())
          .startWorkersAutomatically()
          .build();

  public interface TestWorkflow {

    @WorkflowMethod
    String execute(String taskList);
  }

  /**
   * This workflow reproduces a scenario that was causing a bug with eager activity prefetching. It
   * ensures that we only poll for new activities when there is handler capacity available to
   * process it.
   */
  public static class TestWorkflowImpl implements TestWorkflow {

    @Override
    public String execute(String taskList) {
      MultiplierActivity activity =
          Workflow.newActivityStub(
              MultiplierActivity.class,
              new ActivityOptions.Builder()
                  .setScheduleToStartTimeout(Duration.ofSeconds(10))
                  .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                  .setHeartbeatTimeout(Duration.ofSeconds(1))
                  .setRetryOptions(
                      new RetryOptions.Builder()
                          .setInitialInterval(Duration.ofSeconds(1))
                          .setMaximumAttempts(1)
                          .build())
                  .build());
      List<Promise<Integer>> promises = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        final int value = i;
        promises.add(Async.function(() -> activity.execute(value)));
      }
      Promise.allOf(promises).get();
      return "success";
    }
  }

  public interface MultiplierActivity {

    @ActivityMethod
    int execute(int value);
  }

  public static class SleepyMultiplier implements MultiplierActivity {

    @Override
    public int execute(int value) {
      Activity.heartbeat(value);
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        e.printStackTrace();
      }
      return value * 2;
    }
  }

  /**
   * This test runs a workflow that executes multiple activities in a single handler thread. Test
   * workflow is configured to fail with heartbeat timeout errors in case if activity pollers are
   * too eager to poll tasks before previously fetched tasks are handled.
   */
  @Test
  public void verifyThatActivityIsNotPrefetchedWhenThereIsNoHandlerAvailable() {
    String taskList = testWorkflowRule.getTaskList();
    TestWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                TestWorkflow.class,
                new WorkflowOptions.Builder()
                    .setTaskList(taskList)
                    .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());
    String result = workflow.execute(taskList);
    assertEquals("success", result);
  }
}
