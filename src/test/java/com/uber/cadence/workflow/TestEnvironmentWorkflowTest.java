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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.common.CronSchedule;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestEnvironmentWorkflowTest {
  static final String TASK_LIST = "tasklist";
  static final String CRON_WORKFLOW_ID = "cron_workflow";

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient workflowClient;

  public interface CronW {
    @WorkflowMethod(
      executionStartToCloseTimeoutSeconds = 10,
      workflowId = CRON_WORKFLOW_ID,
      taskList = TASK_LIST
    )
    @CronSchedule("* * * * *")
    void cron();
  }

  public static class CronWImpl implements CronW {
    @Override
    public void cron() {}
  }

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(CronWImpl.class);
    workflowClient = testEnv.newWorkflowClient();

    testEnv.start();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testCronWorkflow() {
    CronW workflow = workflowClient.newWorkflowStub(CronW.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::cron);
    assertEquals(CRON_WORKFLOW_ID, execution.getWorkflowId());

    // start event should have cron schedule
    GetWorkflowExecutionHistoryRequest getRequest =
        new GetWorkflowExecutionHistoryRequest()
            .setDomain(testEnv.getDomain())
            .setExecution(
                new WorkflowExecution()
                    .setWorkflowId(execution.getWorkflowId())
                    .setRunId(execution.getRunId()))
            .setHistoryEventFilterType(HistoryEventFilterType.ALL_EVENT);
    try {
      GetWorkflowExecutionHistoryResponse response =
          workflowClient.getService().GetWorkflowExecutionHistory(getRequest);
      assertEquals(
          "* * * * *",
          response
              .getHistory()
              .getEvents()
              .get(0)
              .getWorkflowExecutionStartedEventAttributes()
              .getCronSchedule());
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }

    // sleep for 61 seconds on server and should expect 2 completed runs
    testEnv.sleep(Duration.ofSeconds(61));
    ListClosedWorkflowExecutionsRequest listRequest =
        new ListClosedWorkflowExecutionsRequest()
            .setDomain(testEnv.getDomain())
            .setExecutionFilter(new WorkflowExecutionFilter().setWorkflowId(CRON_WORKFLOW_ID));
    try {
      ListClosedWorkflowExecutionsResponse listResponse =
          testEnv.getWorkflowService().ListClosedWorkflowExecutions(listRequest);
      Assert.assertEquals(2, listResponse.getExecutions().size());
      for (WorkflowExecutionInfo e : listResponse.getExecutions()) {
        assertTrue(e.isCron());
        assertEquals(WorkflowExecutionCloseStatus.CONTINUED_AS_NEW, e.getCloseStatus());
      }
    } catch (Exception e) {
      fail("no exception expected: " + e.getMessage());
    }
  }
}
