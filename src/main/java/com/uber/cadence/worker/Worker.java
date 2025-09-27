/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.worker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.common.WorkflowExecutionHistory;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.internal.worker.PollerOptions;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.replay.DeciderCache;
import com.uber.cadence.internal.sync.SyncActivityWorker;
import com.uber.cadence.internal.sync.SyncWorkflowWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.internal.worker.Suspendable;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.WorkflowMethod;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import io.opentracing.noop.NoopTracer;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hosts activity and workflow implementations. Uses long poll to receive activity and decision
 * tasks and processes them in a correspondent thread pool.
 */
public final class Worker implements Suspendable {

  private final WorkerOptions options;
  private final String taskList;
  private final SyncWorkflowWorker workflowWorker;
  private final SyncActivityWorker activityWorker;
  private final AtomicBoolean started = new AtomicBoolean();

  /**
   * Creates worker that connects to an instance of the Cadence Service.
   *
   * @param client client to the Cadence Service endpoint.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @param options Options (like {@link PollerOptions} override) for configuring worker.
   */
  Worker(
      WorkflowClient client,
      String taskList,
      WorkerFactoryOptions factoryOptions,
      WorkerOptions options,
      DeciderCache cache,
      String stickyTaskListName,
      Duration stickyDecisionScheduleToStartTimeout,
      ThreadPoolExecutor threadPoolExecutor,
      List<ContextPropagator> contextPropagators) {
    this.taskList = Objects.requireNonNull(taskList);
    options = MoreObjects.firstNonNull(options, WorkerOptions.defaultInstance());
    // try using client options tracer if worker options tracer is NoopTracer
    if (options.getTracer() instanceof NoopTracer) {
      options =
          WorkerOptions.newBuilder(options)
              .setTracer(client.getService().getOptions().getTracer())
              .build();
    }
    this.options = options;

    Scope metricsScope =
        client
            .getOptions()
            .getMetricsScope()
            .tagged(ImmutableMap.of(MetricsTag.TASK_LIST, taskList));

    SingleWorkerOptions activityOptions =
        SingleWorkerOptions.newBuilder()
            .setIdentity(client.getOptions().getIdentity())
            .setDataConverter(client.getOptions().getDataConverter())
            .setTaskExecutorThreadPoolSize(options.getMaxConcurrentActivityExecutionSize())
            .setTaskListActivitiesPerSecond(options.getTaskListActivitiesPerSecond())
            .setPollerOptions(options.getActivityPollerOptions())
            .setMetricsScope(metricsScope)
            .setEnableLoggingInReplay(factoryOptions.isEnableLoggingInReplay())
            .setContextPropagators(contextPropagators)
            .setTracer(options.getTracer())
            .setExecutorWrapper(factoryOptions.getExecutorWrapper())
            .build();
    activityWorker =
        new SyncActivityWorker(
            client.getService(), client.getOptions().getDomain(), taskList, activityOptions);

    SingleWorkerOptions workflowOptions =
        SingleWorkerOptions.newBuilder()
            .setDataConverter(client.getOptions().getDataConverter())
            .setIdentity(client.getOptions().getIdentity())
            .setPollerOptions(options.getWorkflowPollerOptions())
            .setTaskExecutorThreadPoolSize(options.getMaxConcurrentWorkflowExecutionSize())
            .setMetricsScope(metricsScope)
            .setEnableLoggingInReplay(factoryOptions.isEnableLoggingInReplay())
            .setContextPropagators(contextPropagators)
            .setTracer(options.getTracer())
            .setExecutorWrapper(factoryOptions.getExecutorWrapper())
            .build();
    SingleWorkerOptions localActivityOptions =
        SingleWorkerOptions.newBuilder()
            .setDataConverter(client.getOptions().getDataConverter())
            .setIdentity(client.getOptions().getIdentity())
            .setPollerOptions(options.getWorkflowPollerOptions())
            .setTaskExecutorThreadPoolSize(options.getMaxConcurrentLocalActivityExecutionSize())
            .setMetricsScope(metricsScope)
            .setEnableLoggingInReplay(factoryOptions.isEnableLoggingInReplay())
            .setContextPropagators(contextPropagators)
            .setTracer(options.getTracer())
            .setExecutorWrapper(factoryOptions.getExecutorWrapper())
            .build();
    workflowWorker =
        new SyncWorkflowWorker(
            client.getService(),
            client.getOptions().getDomain(),
            taskList,
            this.options.getInterceptorFactory(),
            workflowOptions,
            localActivityOptions,
            activityOptions,
            cache,
            stickyTaskListName,
            stickyDecisionScheduleToStartTimeout,
            threadPoolExecutor);
  }

  SyncWorkflowWorker getWorkflowWorker() {
    return workflowWorker;
  }

  /**
   * Register workflow implementation classes with a worker. Overwrites previously registered types.
   * A workflow implementation class must implement at least one interface with a method annotated
   * with {@link WorkflowMethod}. That method becomes a workflow type that this worker supports.
   *
   * <p>Implementations that share a worker must implement different interfaces as a workflow type
   * is identified by the workflow interface, not by the implementation.
   *
   * <p>The reason for registration accepting workflow class, but not the workflow instance is that
   * workflows are stateful and a new instance is created for each workflow execution.
   */
  public void registerWorkflowImplementationTypes(Class<?>... workflowImplementationClasses) {
    Preconditions.checkState(
        !started.get(),
        "registerWorkflowImplementationTypes is not allowed after worker has started");

    workflowWorker.setWorkflowImplementationTypes(
        new WorkflowImplementationOptions.Builder().build(), workflowImplementationClasses);
  }

  /**
   * Register workflow implementation classes with a worker. Overwrites previously registered types.
   * A workflow implementation class must implement at least one interface with a method annotated
   * with {@link WorkflowMethod}. That method becomes a workflow type that this worker supports.
   *
   * <p>Implementations that share a worker must implement different interfaces as a workflow type
   * is identified by the workflow interface, not by the implementation.
   *
   * <p>The reason for registration accepting workflow class, but not the workflow instance is that
   * workflows are stateful and a new instance is created for each workflow execution.
   */
  public void registerWorkflowImplementationTypes(
      WorkflowImplementationOptions options, Class<?>... workflowImplementationClasses) {
    Preconditions.checkState(
        !started.get(),
        "registerWorkflowImplementationTypes is not allowed after worker has started");

    workflowWorker.setWorkflowImplementationTypes(options, workflowImplementationClasses);
  }

  /**
   * Configures a factory to use when an instance of a workflow implementation is created.
   * !IMPORTANT to provide newly created instances, each time factory is applied.
   *
   * <p>Unless mocking a workflow execution use {@link
   * #registerWorkflowImplementationTypes(Class[])}.
   *
   * @param workflowInterface Workflow interface that this factory implements
   * @param factory factory that when called creates a new instance of the workflow implementation
   *     object.
   * @param <R> type of the workflow object to create.
   */
  public <R> void addWorkflowImplementationFactory(
      WorkflowImplementationOptions options, Class<R> workflowInterface, Func<R> factory) {
    workflowWorker.addWorkflowImplementationFactory(options, workflowInterface, factory);
  }

  /**
   * Configures a factory to use when an instance of a workflow implementation is created. The only
   * valid use for this method is unit testing, specifically to instantiate mocks that implement
   * child workflows. An example of mocking a child workflow:
   *
   * <pre><code>
   *   worker.addWorkflowImplementationFactory(ChildWorkflow.class, () -> {
   *     ChildWorkflow child = mock(ChildWorkflow.class);
   *     when(child.workflow(anyString(), anyString())).thenReturn("result1");
   *     return child;
   *   });
   * </code></pre>
   *
   * <p>Unless mocking a workflow execution use {@link
   * #registerWorkflowImplementationTypes(Class[])}.
   *
   * @param workflowInterface Workflow interface that this factory implements
   * @param factory factory that when called creates a new instance of the workflow implementation
   *     object.
   * @param <R> type of the workflow object to create.
   */
  @VisibleForTesting
  public <R> void addWorkflowImplementationFactory(Class<R> workflowInterface, Func<R> factory) {
    workflowWorker.addWorkflowImplementationFactory(workflowInterface, factory);
  }

  /**
   * Register activity implementation objects with a worker. Overwrites previously registered
   * objects. As activities are reentrant and stateless only one instance per activity type is
   * registered.
   *
   * <p>Implementations that share a worker must implement different interfaces as an activity type
   * is identified by the activity interface, not by the implementation.
   *
   * <p>
   */
  public void registerActivitiesImplementations(Object... activityImplementations) {
    Preconditions.checkState(
        !started.get(),
        "registerActivitiesImplementations is not allowed after worker has started");

    if (activityWorker != null) {
      activityWorker.setActivitiesImplementation(activityImplementations);
      workflowWorker.setActivitiesImplementationToDispatchLocally(activityImplementations);
    }

    workflowWorker.setLocalActivitiesImplementation(activityImplementations);
  }

  void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    workflowWorker.start();
    activityWorker.start();
  }

  void shutdown() {
    activityWorker.shutdown();
    workflowWorker.shutdown();
  }

  void shutdownNow() {
    activityWorker.shutdownNow();
    workflowWorker.shutdownNow();
  }

  boolean isTerminated() {
    return activityWorker.isTerminated() && workflowWorker.isTerminated();
  }

  void awaitTermination(long timeout, TimeUnit unit) {
    long timeoutMillis = InternalUtils.awaitTermination(activityWorker, unit.toMillis(timeout));
    InternalUtils.awaitTermination(workflowWorker, timeoutMillis);
  }

  @Override
  public String toString() {
    return "Worker{" + "options=" + options + '}';
  }

  /**
   * This is an utility method to replay a workflow execution using this particular instance of a
   * worker. This method is useful to troubleshoot workflows by running them in a debugger. To work
   * the workflow implementation type must be registered with this worker. There is no need to call
   * {@link #start()} to be able to call this method.
   *
   * @param history workflow execution history to replay
   * @throws Exception if replay failed for any reason
   */
  public void replayWorkflowExecution(WorkflowExecutionHistory history) throws Exception {
    workflowWorker.queryWorkflowExecution(
        history,
        WorkflowClient.QUERY_TYPE_REPLAY_ONLY,
        String.class,
        String.class,
        new Object[] {});
  }

  /**
   * This is an utility method to replay a workflow execution using this particular instance of a
   * worker. This method is useful to troubleshoot workflows by running them in a debugger. To work
   * the workflow implementation type must be registered with this worker. There is no need to call
   * {@link #start()} to be able to call this method.
   *
   * @param jsonSerializedHistory workflow execution history in JSON format to replay
   * @throws Exception if replay failed for any reason
   */
  public void replayWorkflowExecution(String jsonSerializedHistory) throws Exception {
    WorkflowExecutionHistory history = WorkflowExecutionHistory.fromJson(jsonSerializedHistory);
    replayWorkflowExecution(history);
  }

  public String getTaskList() {
    return taskList;
  }

  @Override
  public void suspendPolling() {
    workflowWorker.suspendPolling();
    activityWorker.suspendPolling();
  }

  @Override
  public void resumePolling() {
    workflowWorker.resumePolling();
    activityWorker.resumePolling();
  }

  @Override
  public boolean isSuspended() {
    return workflowWorker.isSuspended() && activityWorker.isSuspended();
  }

  /**
   * Checks if we have a valid connection to the Cadence cluster, and potentially resets the peer
   * list
   */
  public CompletableFuture<Boolean> isHealthy() {
    return workflowWorker.isHealthy();
  }
}
