/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.testUtils;

import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerFactoryOptions;
import com.uber.cadence.worker.WorkerOptions;
import com.uber.cadence.workflow.interceptors.TracingWorkflowInterceptorFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CadenceTestRule implements TestRule {

  private final Builder builder;
  private CadenceTestContext context;

  private CadenceTestRule(Builder builder) {
    this.builder = builder;
  }

  @Override
  public Statement apply(Statement testCase, Description description) {
    // Unless the test overrides the timeout, apply our own
    Test annotation = description.getAnnotation(Test.class);
    if (TestEnvironment.isDebuggerTimeouts() || (annotation == null || annotation.timeout() > 0)) {
      testCase = Timeout.millis(getDefaultTestTimeout().toMillis()).apply(testCase, description);
    }
    Statement finalStatement = testCase;
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (description.getAnnotation(RequiresDockerService.class) != null && !isDockerService()) {
          throw new AssumptionViolatedException(
              "Skipping test because it requires the Docker service");
        }
        if (description.getAnnotation(RequiresTestService.class) != null && isDockerService()) {
          throw new AssumptionViolatedException(
              "Skipping test because it requires the test service");
        }
        setup(description);
        try {
          finalStatement.evaluate();
        } finally {
          teardown();
        }
      }
    };
  }

  public <T> T newWorkflowStub(Class<T> tClass) {
    return getWorkflowClient().newWorkflowStub(tClass, workflowOptionsBuilder().build());
  }

  public TracingWorkflowInterceptorFactory getTracer() {
    return context.getTracer();
  }

  public WorkflowClient getWorkflowClient() {
    return context.getWorkflowClient();
  }

  public WorkflowClient createWorkflowClient(WorkflowClientOptions options) {
    return context.createWorkflowClient(options);
  }

  public Worker getWorker() {
    return context.getDefaultWorker();
  }

  public Worker getWorker(String tasklist) {
    return context.getOrCreateWorker(tasklist);
  }

  public String getDefaultTaskList() {
    return context.getDefaultTaskList();
  }

  public String getTaskList() {
    return getDefaultTaskList();
  }

  public void start() {
    context.start();
  }

  public void stop() {
    context.stop();
  }

  public void suspendPolling() {
    context.suspendPolling();
  }

  public void resumePolling() {
    context.resumePolling();
  }

  public void registerDelayedCallback(Duration delay, Runnable r) {
    context.registerDelayedCallback(delay, r);
  }

  public void sleep(Duration d) {
    context.sleep(d);
  }

  public long currentTimeMillis() {
    return context.currentTimeMillis();
  }

  public WorkflowOptions.Builder workflowOptionsBuilder() {
    return workflowOptionsBuilder(context.getDefaultTaskList());
  }

  public WorkflowOptions.Builder workflowOptionsBuilder(String taskList) {
    if (TestEnvironment.isDebuggerTimeouts()) {
      return new WorkflowOptions.Builder()
          .setExecutionStartToCloseTimeout(Duration.ofSeconds(1000))
          .setTaskStartToCloseTimeout(Duration.ofSeconds(60))
          .setTaskList(taskList);
    } else {
      return new WorkflowOptions.Builder()
          .setExecutionStartToCloseTimeout(Duration.ofSeconds(30))
          .setTaskStartToCloseTimeout(Duration.ofSeconds(5))
          .setTaskList(taskList);
    }
  }

  public ActivityOptions activityOptions() {
    return activityOptions(context.getDefaultTaskList());
  }

  public ActivityOptions activityOptions(String taskList) {
    if (TestEnvironment.isDebuggerTimeouts()) {
      return new ActivityOptions.Builder()
          .setTaskList(taskList)
          .setScheduleToCloseTimeout(Duration.ofSeconds(1000))
          .setHeartbeatTimeout(Duration.ofSeconds(1000))
          .setScheduleToStartTimeout(Duration.ofSeconds(1000))
          .setStartToCloseTimeout(Duration.ofSeconds(10000))
          .build();
    } else {
      return new ActivityOptions.Builder()
          .setTaskList(taskList)
          .setScheduleToCloseTimeout(Duration.ofSeconds(5))
          .setHeartbeatTimeout(Duration.ofSeconds(5))
          .setScheduleToStartTimeout(Duration.ofSeconds(5))
          .setStartToCloseTimeout(Duration.ofSeconds(10))
          .build();
    }
  }

  public LocalActivityOptions localActivityOptions() {
    if (TestEnvironment.isDebuggerTimeouts()) {
      return new LocalActivityOptions.Builder()
          .setScheduleToCloseTimeout(Duration.ofSeconds(1000))
          .build();
    } else {
      return new LocalActivityOptions.Builder()
          .setScheduleToCloseTimeout(Duration.ofSeconds(5))
          .build();
    }
  }

  private void setup(Description description) {
    String testMethod = description.getMethodName();
    String defaultTaskList =
        description.getClassName() + "-" + testMethod + "-" + UUID.randomUUID();

    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder(builder.clientOptions).setDomain(builder.domain).build();

    if (isDockerService()) {
      this.context =
          CadenceTestContext.forRealService(
              clientOptions, defaultTaskList, builder.workerFactoryOptions, builder.workerOptions);
    } else {
      this.context =
          CadenceTestContext.forTestService(
              builder.testWorkflowEnvironmentProvider,
              clientOptions,
              defaultTaskList,
              builder.workerFactoryOptions,
              builder.workerOptions);
    }

    if (!builder.activities.isEmpty() || !builder.workflowTypes.isEmpty()) {
      Worker defaultWorker = context.getDefaultWorker();
      if (!builder.activities.isEmpty()) {
        defaultWorker.registerActivitiesImplementations(builder.activities.toArray());
      }
      if (!builder.workflowTypes.isEmpty()) {
        defaultWorker.registerWorkflowImplementationTypes(
            builder.workflowTypes.stream().toArray(Class[]::new));
      }
    }
    if (builder.startWorkers) {
      context.start();
    }
  }

  private void teardown() {
    context.stop();
    this.context = null;
  }

  private Duration getDefaultTestTimeout() {
    if (!builder.timeout.isZero()) {
      return builder.timeout;
    }
    if (TestEnvironment.isDebuggerTimeouts()) {
      return Duration.ofSeconds(500);
    }
    if (isDockerService()) {
      return Duration.ofSeconds(30);
    }
    return Duration.ofSeconds(10);
  }

  public boolean isDockerService() {
    return TestEnvironment.isUseDockerService();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<Class<?>> workflowTypes = new ArrayList<>();
    private List<Object> activities = new ArrayList<>();
    private WorkerFactoryOptions workerFactoryOptions = WorkerFactoryOptions.newBuilder().build();
    private WorkerOptions workerOptions;
    private Function<TestEnvironmentOptions, TestWorkflowEnvironment>
        testWorkflowEnvironmentProvider = TestWorkflowEnvironment::newInstance;
    private WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder().build();

    private boolean startWorkers = false;
    private Duration timeout = Duration.ZERO;
    private String domain = TestEnvironment.DOMAIN;

    public Builder withWorkflowTypes(Class<?>... workflowImpls) {
      workflowTypes = Arrays.asList(workflowImpls);
      return this;
    }

    public Builder withActivities(Object... activities) {
      this.activities = Arrays.asList(activities);
      return this;
    }

    public Builder withWorkerFactoryOptions(WorkerFactoryOptions options) {
      this.workerFactoryOptions = options;
      return this;
    }

    public Builder withWorkerOptions(WorkerOptions options) {
      this.workerOptions = options;
      return this;
    }

    public Builder withTimeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder withDomain(String domain) {
      this.domain = domain;
      return this;
    }

    public Builder withClientOptions(WorkflowClientOptions options) {
      this.clientOptions = options;
      return this;
    }

    public Builder startWorkersAutomatically() {
      startWorkers = true;
      return this;
    }

    public Builder withTestEnvironmentProvider(
        Function<TestEnvironmentOptions, TestWorkflowEnvironment> testEnvironmentProvider) {
      this.testWorkflowEnvironmentProvider = testEnvironmentProvider;
      return this;
    }

    public CadenceTestRule build() {
      return new CadenceTestRule(this);
    }
  }
}
