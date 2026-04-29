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

package com.uber.cadence.internal.sync;

import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.replay.DeciderCache;
import com.uber.cadence.internal.replay.ReplayDecisionTaskHandler;
import com.uber.cadence.internal.worker.DecisionTaskHandler;
import com.uber.cadence.internal.worker.LocalActivityWorker;
import com.uber.cadence.internal.worker.LocallyDispatchedActivityWorker;
import com.uber.cadence.internal.worker.LocallyDispatchedActivityWorker.Task;
import com.uber.cadence.internal.worker.NoopSuspendableWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.internal.worker.SuspendableWorker;
import com.uber.cadence.internal.worker.WorkflowWorker;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.worker.ExecutorWrapper;
import com.uber.cadence.worker.WorkflowImplementationOptions;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.WorkflowInterceptor;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

/** Workflow worker that supports POJO workflow implementations. */
public class SyncWorkflowWorker implements SuspendableWorker {

  private final WorkflowWorker workflowWorker;
  private final LocalActivityWorker laWorker;
  private final POJOWorkflowImplementationFactory factory;
  private final DataConverter dataConverter;
  private final POJOActivityTaskHandler laTaskHandler;
  private final ScheduledExecutorService heartbeatExecutor;
  private final ScheduledExecutorService ldaHeartbeatExecutor;
  private SuspendableWorker ldaWorker;
  private POJOActivityTaskHandler ldaTaskHandler;
  private final IWorkflowService service;

  public SyncWorkflowWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      Function<WorkflowInterceptor, WorkflowInterceptor> interceptorFactory,
      SingleWorkerOptions workflowOptions,
      SingleWorkerOptions localActivityOptions,
      SingleWorkerOptions locallyDispatchedActivityOptions,
      DeciderCache cache,
      String stickyTaskListName,
      ThreadPoolExecutor workflowThreadPool) {
    Objects.requireNonNull(workflowThreadPool);
    this.dataConverter = workflowOptions.getDataConverter();
    this.service = service;

    // heartbeat executors
    ExecutorWrapper executorWrapper = localActivityOptions.getExecutorWrapper();
    heartbeatExecutor = executorWrapper.wrap(Executors.newScheduledThreadPool(4));
    ldaHeartbeatExecutor = executorWrapper.wrap(Executors.newScheduledThreadPool(4));

    factory =
        new POJOWorkflowImplementationFactory(
            workflowOptions.getDataConverter(),
            workflowThreadPool,
            interceptorFactory,
            cache,
            workflowOptions.getContextPropagators(),
            workflowOptions.getTracer());

    laTaskHandler =
        new POJOActivityTaskHandler(
            service, domain, localActivityOptions.getDataConverter(), heartbeatExecutor);
    laWorker = new LocalActivityWorker(domain, taskList, localActivityOptions, laTaskHandler);

    DecisionTaskHandler taskHandler =
        new ReplayDecisionTaskHandler(
            domain,
            factory,
            cache,
            workflowOptions,
            stickyTaskListName,
            workflowOptions.getStickyTaskListScheduleToStartTimeout(),
            service,
            laWorker.getLocalActivityTaskPoller());

    Function<Task, Boolean> locallyDispatchedActivityTaskPoller = null;
    // do not dispatch locally if TaskListActivitiesPerSecond is set
    if (locallyDispatchedActivityOptions.getTaskListActivitiesPerSecond() == 0) {
      ldaTaskHandler =
          new POJOActivityTaskHandler(
              service,
              domain,
              locallyDispatchedActivityOptions.getDataConverter(),
              ldaHeartbeatExecutor);
      ldaWorker =
          new LocallyDispatchedActivityWorker(
              service, domain, taskList, locallyDispatchedActivityOptions, ldaTaskHandler);
      locallyDispatchedActivityTaskPoller =
          ((LocallyDispatchedActivityWorker) ldaWorker).getLocallyDispatchedActivityTaskPoller();
    } else {
      ldaWorker = new NoopSuspendableWorker();
    }

    workflowWorker =
        new WorkflowWorker(
            service,
            domain,
            taskList,
            workflowOptions,
            taskHandler,
            locallyDispatchedActivityTaskPoller,
            stickyTaskListName);
  }

  public void setWorkflowImplementationTypes(
      WorkflowImplementationOptions options, Class<?>[] workflowImplementationTypes) {
    factory.setWorkflowImplementationTypes(options, workflowImplementationTypes);
  }

  public <R> void addWorkflowImplementationFactory(
      WorkflowImplementationOptions options, Class<R> clazz, Func<R> factory) {
    this.factory.addWorkflowImplementationFactory(options, clazz, factory);
  }

  public <R> void addWorkflowImplementationFactory(Class<R> clazz, Func<R> factory) {
    this.factory.addWorkflowImplementationFactory(clazz, factory);
  }

  public void setLocalActivitiesImplementation(Object... activitiesImplementation) {
    this.laTaskHandler.setLocalActivitiesImplementation(activitiesImplementation);
  }

  public void setActivitiesImplementationToDispatchLocally(Object... activitiesImplementation) {
    if (this.ldaTaskHandler != null) {
      this.ldaTaskHandler.setActivitiesImplementation(activitiesImplementation);
    }
  }

  @Override
  public void start() {
    workflowWorker.start();
    // workflowWorker doesn't start if no types are registered with it. In that case we don't need
    // to start LocalActivity Worker.
    if (workflowWorker.isStarted()) {
      laWorker.start();
      ldaWorker.start();
    }
  }

  @Override
  public boolean isStarted() {
    return workflowWorker.isStarted() && laWorker.isStarted() && ldaWorker.isStarted();
  }

  @Override
  public boolean isShutdown() {
    return workflowWorker.isShutdown() && laWorker.isShutdown() && ldaWorker.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return workflowWorker.isTerminated()
        && laWorker.isTerminated()
        && ldaHeartbeatExecutor.isTerminated()
        && ldaWorker.isTerminated();
  }

  @Override
  public void shutdown() {
    laWorker.shutdown();
    ldaHeartbeatExecutor.shutdown();
    ldaWorker.shutdown();
    workflowWorker.shutdown();
  }

  @Override
  public void shutdownNow() {
    laWorker.shutdownNow();
    ldaHeartbeatExecutor.shutdownNow();
    ldaWorker.shutdownNow();
    workflowWorker.shutdownNow();
  }

  @Override
  public void awaitTermination(long timeout, TimeUnit unit) {
    long timeoutMillis = InternalUtils.awaitTermination(laWorker, unit.toMillis(timeout));
    timeoutMillis = InternalUtils.awaitTermination(ldaHeartbeatExecutor, timeoutMillis);
    timeoutMillis = InternalUtils.awaitTermination(ldaWorker, timeoutMillis);
    InternalUtils.awaitTermination(workflowWorker, timeoutMillis);
  }

  @Override
  public void suspendPolling() {
    workflowWorker.suspendPolling();
    laWorker.suspendPolling();
    ldaWorker.suspendPolling();
  }

  @Override
  public void resumePolling() {
    workflowWorker.resumePolling();
    laWorker.resumePolling();
    ldaWorker.resumePolling();
  }

  @Override
  public boolean isSuspended() {
    return workflowWorker.isSuspended() && laWorker.isSuspended() && ldaWorker.isSuspended();
  }

  public <R> R queryWorkflowExecution(
      WorkflowExecutionHistory history,
      String queryType,
      Class<R> resultClass,
      Type resultType,
      Object[] args)
      throws Exception {
    byte[] serializedArgs = dataConverter.toData(args);
    byte[] result = workflowWorker.queryWorkflowExecution(history, queryType, serializedArgs);
    return dataConverter.fromData(result, resultClass, resultType);
  }

  public CompletableFuture<Boolean> isHealthy() {
    return service.isHealthy();
  }
}
