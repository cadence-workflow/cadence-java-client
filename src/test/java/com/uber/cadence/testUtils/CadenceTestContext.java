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

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.internal.worker.PollerOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.cadence.worker.WorkerFactoryOptions;
import com.uber.cadence.worker.WorkerOptions;
import com.uber.cadence.workflow.interceptors.TracingWorkflowInterceptorFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CadenceTestContext {

  private final Map<String, Worker> workers = new HashMap<>();
  private List<ScheduledFuture<?>> delayedCallbacks = new ArrayList<>();
  private final IWorkflowService wfService;
  private final WorkflowClient workflowClient;
  private final String defaultTaskList;
  private final TracingWorkflowInterceptorFactory tracer;
  private final WorkerOptions workerOptions;
  private WorkerFactory workerFactory;
  // Real Service only
  private ScheduledExecutorService scheduledExecutor;
  // Test service only
  private TestWorkflowEnvironment testEnvironment;

  private CadenceTestContext(
      WorkflowClient workflowClient,
      String defaultTaskList,
      TracingWorkflowInterceptorFactory tracer,
      WorkerFactory workerFactory,
      ScheduledExecutorService scheduledExecutor,
      TestWorkflowEnvironment testEnvironment,
      WorkerOptions workerOptions) {
    this.wfService = workflowClient.getService();
    this.workflowClient = workflowClient;
    this.defaultTaskList = defaultTaskList;
    this.tracer = tracer;
    this.workerFactory = workerFactory;
    this.scheduledExecutor = scheduledExecutor;
    this.testEnvironment = testEnvironment;
    this.workerOptions = workerOptions;
  }

  public Worker getDefaultWorker() {
    return getOrCreateWorker(getDefaultTaskList());
  }

  public Worker getOrCreateWorker(String taskList) {
    return workers.computeIfAbsent(taskList, this::createWorker);
  }

  public void start() {
    if (isRealService()) {
      workerFactory.start();
    } else {
      testEnvironment.start();
    }
  }

  public void stop() {
    if (!workerFactory.isStarted() || workerFactory.isTerminated() || workerFactory.isShutdown()) {
      return;
    }
    if (isRealService()) {
      workerFactory.shutdown();
      workerFactory.awaitTermination(1, TimeUnit.SECONDS);
      for (ScheduledFuture<?> result : delayedCallbacks) {
        if (result.isDone() && !result.isCancelled()) {
          try {
            result.get();
          } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted, some assertions may not have run", e);
          } catch (ExecutionException e) {
            if (e.getCause() instanceof AssertionError) {
              throw (AssertionError) e.getCause();
            } else {
              throw new RuntimeException("Failed to complete callback", e.getCause());
            }
          }
        }
      }
      wfService.close();
    } else {
      testEnvironment.shutdown();
      testEnvironment.awaitTermination(1, TimeUnit.SECONDS);
    }
    if (tracer != null) {
      tracer.assertExpected();
    }
  }

  public void suspendPolling() {
    workerFactory.suspendPolling();
  }

  public void resumePolling() {
    workerFactory.resumePolling();
  }

  public void registerDelayedCallback(Duration delay, Runnable r) {
    if (isRealService()) {
      ScheduledFuture<?> result =
          scheduledExecutor.schedule(r, delay.toMillis(), TimeUnit.MILLISECONDS);
      delayedCallbacks.add(result);
    } else {
      testEnvironment.registerDelayedCallback(delay, r);
    }
  }

  public void sleep(Duration d) {
    if (isRealService()) {
      try {
        Thread.sleep(d.toMillis());
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted", e);
      }
    } else {
      testEnvironment.sleep(d);
    }
  }

  public long currentTimeMillis() {
    if (isRealService()) {
      return System.currentTimeMillis();
    } else {
      return testEnvironment.currentTimeMillis();
    }
  }

  public String getDefaultTaskList() {
    return defaultTaskList;
  }

  public WorkflowClient getWorkflowClient() {
    return workflowClient;
  }

  public WorkflowClient createWorkflowClient(WorkflowClientOptions options) {
    if (isRealService()) {
      return WorkflowClient.newInstance(getWorkflowClient().getService(), options);
    } else {
      return testEnvironment.newWorkflowClient(options);
    }
  }

  public TracingWorkflowInterceptorFactory getTracer() {
    return tracer;
  }

  private boolean isRealService() {
    return testEnvironment == null;
  }

  private Worker createWorker(String taskList) {
    if (isRealService()) {
      WorkerOptions options =
          workerOptions != null
              ? workerOptions
              : WorkerOptions.newBuilder()
                  .setActivityPollerOptions(
                      PollerOptions.newBuilder().setPollThreadCount(5).build())
                  .setMaxConcurrentActivityExecutionSize(1000)
                  .setInterceptorFactory(tracer)
                  .build();
      return workerFactory.newWorker(taskList, options);
    } else {
      if (workerOptions != null) {
        return testEnvironment.newWorker(
            taskList, builder -> applyWorkerOptions(builder, workerOptions));
      } else {
        return testEnvironment.newWorker(taskList);
      }
    }
  }

  private static WorkerOptions.Builder applyWorkerOptions(
      WorkerOptions.Builder builder, WorkerOptions options) {
    if (options.getMaxConcurrentActivityExecutionSize() != 0) {
      builder.setMaxConcurrentActivityExecutionSize(
          options.getMaxConcurrentActivityExecutionSize());
    }
    if (options.getMaxConcurrentWorkflowExecutionSize() != 0) {
      builder.setMaxConcurrentWorkflowExecutionSize(
          options.getMaxConcurrentWorkflowExecutionSize());
    }
    if (options.getMaxConcurrentLocalActivityExecutionSize() != 0) {
      builder.setMaxConcurrentLocalActivityExecutionSize(
          options.getMaxConcurrentLocalActivityExecutionSize());
    }
    if (options.getActivityPollerOptions() != null) {
      builder.setActivityPollerOptions(options.getActivityPollerOptions());
    }
    if (options.getWorkflowPollerOptions() != null) {
      builder.setWorkflowPollerOptions(options.getWorkflowPollerOptions());
    }
    return builder;
  }

  public static CadenceTestContext forTestService(
      Function<TestEnvironmentOptions, TestWorkflowEnvironment> envFactory,
      WorkflowClientOptions clientOptions,
      String defaultTaskList,
      WorkerFactoryOptions workerFactoryOptions,
      WorkerOptions workerOptions) {
    TracingWorkflowInterceptorFactory tracer = new TracingWorkflowInterceptorFactory();

    TestEnvironmentOptions testOptions =
        new TestEnvironmentOptions.Builder()
            .setWorkflowClientOptions(clientOptions)
            .setInterceptorFactory(tracer)
            .setWorkerFactoryOptions(workerFactoryOptions)
            .build();
    TestWorkflowEnvironment testEnvironment = envFactory.apply(testOptions);
    return new CadenceTestContext(
        testEnvironment.newWorkflowClient(),
        defaultTaskList,
        tracer,
        testEnvironment.getWorkerFactory(),
        null,
        testEnvironment,
        workerOptions);
  }

  public static CadenceTestContext forRealService(
      WorkflowClientOptions clientOptions,
      String defaultTaskList,
      WorkerFactoryOptions workerFactoryOptions,
      WorkerOptions workerOptions) {
    TracingWorkflowInterceptorFactory tracer = new TracingWorkflowInterceptorFactory();

    IWorkflowService wfService = TestEnvironment.getDockerService();
    WorkflowClient workflowClient = WorkflowClient.newInstance(wfService, clientOptions);
    WorkerFactory workerFactory = new WorkerFactory(workflowClient, workerFactoryOptions);
    ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    return new CadenceTestContext(
        workflowClient,
        defaultTaskList,
        tracer,
        workerFactory,
        scheduledExecutor,
        null,
        workerOptions);
  }
}
