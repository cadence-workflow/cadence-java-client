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
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.sync.ActivityExecutionContext;
import com.uber.cadence.worker.WorkerOptions;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;

public class SimpleManualCompletionTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkerOptions(
              WorkerOptions.newBuilder().setMaxConcurrentActivityExecutionSize(1).build())
          .setWorkflowTypes(TestWorkflowImpl.class)
          .setActivityImplementations(new SimpleActivity())
          .build();

  public interface TestWorkflow {
    @WorkflowMethod
    String execute();
  }

  public static class TestWorkflowImpl implements TestWorkflow {
    @Override
    public String execute() {
      TestActivity activity =
          Workflow.newActivityStub(
              TestActivity.class,
              new ActivityOptions.Builder()
                  .setScheduleToStartTimeout(Duration.ofSeconds(10))
                  .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                  .setRetryOptions(
                      new RetryOptions.Builder()
                          .setMaximumAttempts(1)
                          .setInitialInterval(Duration.ofSeconds(1))
                          .build())
                  .build());
      return activity.execute();
    }
  }

  public interface TestActivity {
    @ActivityMethod
    String execute();
  }

  public static class SimpleActivity implements TestActivity {
    private static final AtomicBoolean completed = new AtomicBoolean(false);

    @Override
    public String execute() {
      ActivityExecutionContext context = Activity.getExecutionContext();
      ActivityCompletionClient completionClient = context.useLocalManualCompletion();

      // Complete immediately
      new Thread(
              () -> {
                try {
                  Thread.sleep(100);
                  completed.set(true);
                  completionClient.complete(context.getTaskToken(), "success");
                } catch (Exception e) {
                  e.printStackTrace();
                }
              })
          .start();

      return "should-not-be-returned";
    }
  }

  @Test
  public void testSimpleManualCompletion() {
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    TestWorkflow workflow =
        workflowClient.newWorkflowStub(
            TestWorkflow.class,
            new WorkflowOptions.Builder()
                .setTaskList(testWorkflowRule.getTaskList())
                .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
                .build());
    String result = workflow.execute();
    assertEquals("success", result);
  }
}
