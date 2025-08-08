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

import static com.uber.cadence.testUtils.TestEnvironment.DOMAIN;
import static com.uber.cadence.testUtils.TestEnvironment.DOMAIN2;
import static junit.framework.TestCase.fail;

import com.uber.cadence.*;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.common.CronSchedule;
import com.uber.cadence.internal.worker.PollerOptions;
import com.uber.cadence.migration.MigrationActivitiesImpl;
import com.uber.cadence.migration.MigrationIWorkflowService;
import com.uber.cadence.migration.MigrationInterceptorFactory;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testUtils.CadenceTestRule;
import com.uber.cadence.testUtils.RequiresDockerService;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.cadence.worker.WorkerFactoryOptions;
import com.uber.cadence.worker.WorkerOptions;
import com.uber.cadence.workflow.interceptors.TracingWorkflowInterceptorFactory;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import org.junit.*;

public class WorkflowMigrationTest {
  private WorkflowClient migrationWorkflowClient, workflowClientCurr, workflowClientNew;
  private static final String TASKLIST = "TASKLIST";
  private TracingWorkflowInterceptorFactory tracer;
  WorkerFactory factoryCurr, factoryNew;
  Worker workerCurr, workerNew;

  @Rule public CadenceTestRule testRuleCur = CadenceTestRule.builder().withDomain(DOMAIN).build();

  @Rule public CadenceTestRule testRuleNew = CadenceTestRule.builder().withDomain(DOMAIN2).build();

  @Before
  public void setUp() {
    IWorkflowService serviceCur = testRuleCur.getWorkflowClient().getService();
    IWorkflowService serviceNew = testRuleNew.getWorkflowClient().getService();
    workflowClientCurr =
        WorkflowClient.newInstance(
            serviceCur, WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build());
    workflowClientNew =
        WorkflowClient.newInstance(
            serviceNew, WorkflowClientOptions.newBuilder().setDomain(DOMAIN2).build());
    MigrationIWorkflowService migrationService =
        new MigrationIWorkflowService(
            serviceCur, DOMAIN,
            serviceNew, DOMAIN2);
    migrationWorkflowClient =
        WorkflowClient.newInstance(
            migrationService, WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build());
    WorkerFactoryOptions factoryOptions = WorkerFactoryOptions.newBuilder().build();

    // tracer interceptor Factory
    tracer = new TracingWorkflowInterceptorFactory();

    // migration interceptor
    MigrationInterceptorFactory migrator = new MigrationInterceptorFactory(workflowClientNew);

    // current domain worker
    factoryCurr = new WorkerFactory(workflowClientCurr, factoryOptions);
    workerCurr =
        factoryCurr.newWorker(
            TASKLIST,
            WorkerOptions.newBuilder()
                .setActivityPollerOptions(PollerOptions.newBuilder().setPollThreadCount(5).build())
                .setMaxConcurrentActivityExecutionSize(1000)
                .setInterceptorFactory(migrator.andThen(tracer))
                .build());
    workerCurr.registerWorkflowImplementationTypes(
        CronWorkflowImpl.class, ContinueAsNewWorkflowImpl.class);
    workerCurr.registerActivitiesImplementations(
        new MigrationActivitiesImpl(workflowClientCurr, workflowClientNew));
    factoryCurr.start();

    // new domain worker
    factoryNew = new WorkerFactory(workflowClientNew, factoryOptions);
    workerNew =
        factoryNew.newWorker(
            TASKLIST,
            WorkerOptions.newBuilder()
                .setActivityPollerOptions(PollerOptions.newBuilder().setPollThreadCount(5).build())
                .setMaxConcurrentActivityExecutionSize(1000)
                .setInterceptorFactory(tracer)
                .build());
    workerNew.registerWorkflowImplementationTypes(
        CronWorkflowImpl.class, ContinueAsNewWorkflowImpl.class);
    factoryNew.start();
  }

  @After
  public void tearDown() throws Throwable {
    factoryCurr.shutdown();
    factoryNew.shutdown();
  }

  public interface CronWorkflow {
    @WorkflowMethod(
      taskList = TASKLIST,
      workflowIdReusePolicy = WorkflowIdReusePolicy.RejectDuplicate,
      executionStartToCloseTimeoutSeconds = 10
    )
    @CronSchedule("* * * * *")
    String execute(String testName);
  }

  public static class CronWorkflowImpl implements CronWorkflow {
    @Override
    public String execute(String testName) {
      return "Cron Workflow Completed";
    }
  }

  public interface ContinueAsNewWorkflow {
    @WorkflowMethod(
      taskList = TASKLIST,
      workflowIdReusePolicy = WorkflowIdReusePolicy.RejectDuplicate,
      executionStartToCloseTimeoutSeconds = 10
    )
    void execute(int iter);
  }

  public static class ContinueAsNewWorkflowImpl implements ContinueAsNewWorkflow {
    @Override
    public void execute(int iter) {
      Workflow.continueAsNew(iter + 1);
    }
  }

  @Test
  @RequiresDockerService // test service doesn't support describe API yet
  public void cronWorkflowMigration() {
    String workflowID = UUID.randomUUID().toString();
    try {
      workflowClientCurr
          .newWorkflowStub(
              CronWorkflow.class, new WorkflowOptions.Builder().setWorkflowId(workflowID).build())
          .execute("for test");
    } catch (CancellationException e) {
      try {
        getWorkflowHistory(workflowClientNew, workflowID);
      } catch (Exception eDesc) {
        fail("fail to describe workflow execution in new domain: " + eDesc);
      }
    }
  }

  @Test
  @RequiresDockerService // test service doesn't support describe API yet
  public void continueAsNewWorkflowMigration() {
    String workflowID = UUID.randomUUID().toString();
    try {
      workflowClientCurr
          .newWorkflowStub(
              ContinueAsNewWorkflow.class,
              new WorkflowOptions.Builder().setWorkflowId(workflowID).build())
          .execute(0);
    } catch (CancellationException e) {
      try {
        getWorkflowHistory(workflowClientNew, workflowID);
      } catch (Exception eDesc) {
        fail("fail to describe workflow execution in new domain: " + eDesc);
      }
    }
  }

  private GetWorkflowExecutionHistoryResponse getWorkflowHistory(
      WorkflowClient wc, String workflowID) throws BaseError {
    return wc.getService()
        .GetWorkflowExecutionHistory(
            new GetWorkflowExecutionHistoryRequest()
                .setExecution(new WorkflowExecution().setWorkflowId(workflowID))
                .setDomain(wc.getOptions().getDomain()));
  }
}
