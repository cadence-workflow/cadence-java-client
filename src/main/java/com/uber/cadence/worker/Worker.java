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

package com.uber.cadence.worker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.sync.SyncActivityWorker;
import com.uber.cadence.internal.sync.SyncWorkflowWorker;
import com.uber.cadence.internal.worker.SingleWorkerOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.worker.WorkerOptions.Builder;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.WorkflowMethod;
import com.uber.m3.util.ImmutableMap;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Hosts activity and workflow implementations. Uses long poll to receive activity and decision
 * tasks and processes them in a correspondent thread pool.
 */
public final class Worker {

  private final WorkerOptions options;
  private final String taskList;
  private final SyncWorkflowWorker workflowWorker;
  private final SyncActivityWorker activityWorker;
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();

  /**
   * Creates worker that connects to the local instance of the Cadence Service that listens on a
   * default port (7933).
   *
   * @param domain domain that worker uses to poll.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   */
  private Worker(String domain, String taskList) {
    this(domain, taskList, null);
  }

  /**
   * Creates worker that connects to the local instance of the Cadence Service that listens on a
   * default port (7933).
   *
   * @param domain domain that worker uses to poll.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @param options Options (like {@link DataConverter} override) for configuring worker.
   */
  private Worker(String domain, String taskList, WorkerOptions options) {
    this(new WorkflowServiceTChannel(), domain, taskList, options);
  }

  /**
   * Creates worker that connects to an instance of the Cadence Service.
   *
   * @param host of the Cadence Service endpoint
   * @param port of the Cadence Service endpoint
   * @param domain domain that worker uses to poll.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   */
  private Worker(String host, int port, String domain, String taskList) {
    this(new WorkflowServiceTChannel(host, port), domain, taskList, null);
  }

  /**
   * Creates worker that connects to an instance of the Cadence Service.
   *
   * @param host of the Cadence Service endpoint
   * @param port of the Cadence Service endpoint
   * @param domain domain that worker uses to poll.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @param options Options (like {@link DataConverter} override) for configuring worker.
   */
  private Worker(String host, int port, String domain, String taskList, WorkerOptions options) {
    this(new WorkflowServiceTChannel(host, port), domain, taskList, options);
  }

  /**
   * Creates worker that connects to an instance of the Cadence Service.
   *
   * @param service client to the Cadence Service endpoint.
   * @param domain domain that worker uses to poll.
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @param options Options (like {@link DataConverter} override) for configuring worker.
   */
  private Worker(IWorkflowService service, String domain, String taskList, WorkerOptions options) {
    Objects.requireNonNull(service, "service");
    Objects.requireNonNull(domain, "domain");
    this.taskList = Objects.requireNonNull(taskList, "taskList");
    if (options == null) {
      options = new Builder().build();
    }
    this.options = options;
    SingleWorkerOptions activityOptions = toActivityOptions(options, domain, taskList);
    if (!options.isDisableActivityWorker()) {
      activityWorker = new SyncActivityWorker(service, domain, taskList, activityOptions);
    } else {
      activityWorker = null;
    }
    SingleWorkerOptions workflowOptions = toWorkflowOptions(options, domain, taskList);
    if (!options.isDisableWorkflowWorker()) {
      workflowWorker =
          new SyncWorkflowWorker(
              service,
              domain,
              taskList,
              options.getInterceptorFactory(),
              workflowOptions,
              options.getMaxWorkflowThreads());
    } else {
      workflowWorker = null;
    }
  }

  private SingleWorkerOptions toActivityOptions(
      WorkerOptions options, String domain, String taskList) {
    Map<String, String> tags =
        new ImmutableMap.Builder<String, String>(2)
            .put(MetricsTag.DOMAIN, domain)
            .put(MetricsTag.TASK_LIST, taskList)
            .build();
    return new SingleWorkerOptions.Builder()
        .setDataConverter(options.getDataConverter())
        .setIdentity(options.getIdentity())
        .setPollerOptions(options.getActivityPollerOptions())
        .setReportCompletionRetryOptions(options.getReportActivityCompletionRetryOptions())
        .setReportFailureRetryOptions(options.getReportActivityFailureRetryOptions())
        .setTaskExecutorThreadPoolSize(options.getMaxConcurrentActivityExecutionSize())
        .setMetricsScope(options.getMetricsScope().tagged(tags))
        .setEnableLoggingInReplay(options.getEnableLoggingInReplay())
        .build();
  }

  private SingleWorkerOptions toWorkflowOptions(
      WorkerOptions options, String domain, String taskList) {
    Map<String, String> tags =
        new ImmutableMap.Builder<String, String>(2)
            .put(MetricsTag.DOMAIN, domain)
            .put(MetricsTag.TASK_LIST, taskList)
            .build();
    return new SingleWorkerOptions.Builder()
        .setDataConverter(options.getDataConverter())
        .setIdentity(options.getIdentity())
        .setPollerOptions(options.getWorkflowPollerOptions())
        .setReportCompletionRetryOptions(options.getReportWorkflowCompletionRetryOptions())
        .setReportFailureRetryOptions(options.getReportWorkflowFailureRetryOptions())
        .setTaskExecutorThreadPoolSize(options.getMaxConcurrentWorklfowExecutionSize())
        .setMetricsScope(options.getMetricsScope().tagged(tags))
        .setEnableLoggingInReplay(options.getEnableLoggingInReplay())
        .build();
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
    if (workflowWorker == null) {
      throw new IllegalStateException("disableWorkflowWorker is set in worker options");
    }
    checkNotStarted();
    workflowWorker.setWorkflowImplementationTypes(workflowImplementationClasses);
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
    if (activityWorker == null) {
      throw new IllegalStateException("disableActivityWorker is set in worker options");
    }
    checkNotStarted();
    activityWorker.setActivitiesImplementation(activityImplementations);
  }

  private void checkNotStarted() {
    if (started.get()) {
      throw new IllegalStateException("already started");
    }
  }

  private void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    if (workflowWorker != null) {
      workflowWorker.start();
    }
    if (activityWorker != null) {
      activityWorker.start();
    }
  }

  public boolean isStarted() {
    return started.get();
  }

  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Shutdown a worker, waiting for activities to complete execution up to the specified timeout.
   */
  private void shutdown(Duration timeout) {
    try {
      long time = System.currentTimeMillis();
      if (activityWorker != null) {
        activityWorker.shutdownAndAwaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
      }
      if (workflowWorker != null) {
        long left = timeout.toMillis() - (System.currentTimeMillis() - time);
        workflowWorker.shutdownAndAwaitTermination(left, TimeUnit.MILLISECONDS);
      }
      closed.set(true);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "Worker{" + "options=" + options + '}';
  }

  /**
   * This is an utility method to query a workflow execution using this particular instance of a
   * worker. It gets a history from a Cadence service, replays a workflow code and then runs the
   * query. This method is useful to troubleshoot workflows by running them in a debugger. To work
   * the workflow implementation type must be registered with this worker. In most cases using
   * {@link WorkflowClient} to query workflows is preferable, as it doesn't require workflow
   * implementation code to be available. There is no need to call {@link #start()} to be able to
   * call this method.
   *
   * @param execution workflow execution to replay and then query locally
   * @param queryType query type to execute
   * @param returnType return type of the query result
   * @param args query arguments
   * @param <R> type of the query result
   * @return query result
   * @throws Exception if replay failed for any reason
   */
  public <R> R queryWorkflowExecution(
      WorkflowExecution execution, String queryType, Class<R> returnType, Object... args)
      throws Exception {
    return queryWorkflowExecution(execution, queryType, returnType, returnType, args);
  }

  /**
   * This is an utility method to query a workflow execution using this particular instance of a
   * worker. It gets a history from a Cadence service, replays a workflow code and then runs the
   * query. This method is useful to troubleshoot workflows by running them in a debugger. To work
   * the workflow implementation type must be registered with this worker. In most cases using
   * {@link WorkflowClient} to query workflows is preferable, as it doesn't require workflow
   * implementation code to be available. There is no need to call {@link #start()} to be able to
   * call this method.
   *
   * @param execution workflow execution to replay and then query locally
   * @param queryType query type to execute
   * @param resultClass return class of the query result
   * @param resultType return type of the query result. Useful when resultClass is a generic type.
   * @param args query arguments
   * @param <R> type of the query result
   * @return query result
   * @throws Exception if replay failed for any reason
   */
  public <R> R queryWorkflowExecution(
      WorkflowExecution execution,
      String queryType,
      Class<R> resultClass,
      Type resultType,
      Object... args)
      throws Exception {
    if (workflowWorker == null) {
      throw new IllegalStateException("disableWorkflowWorker is set in worker options");
    }
    return workflowWorker.queryWorkflowExecution(
        execution, queryType, resultClass, resultType, args);
  }

  public String getTaskList() {
    return taskList;
  }

  public static final class Factory {

    private final ArrayList<Worker> workers = new ArrayList<>();
    private final Supplier<IWorkflowService> getWorkFlowService;
    private String domain;
    private State state = State.Initial;

    private final String statusErrorMessage =
        "attempted to %s while in %s state. Acceptable States: %s";

    public Factory(String domain) {
      this(() -> new WorkflowServiceTChannel(), domain);
    }

    public Factory(String host, int port, String domain) {
      this(() -> new WorkflowServiceTChannel(host, port), domain);
    }

    public Factory(Supplier<IWorkflowService> getWorkFlowService, String domain) {
      Preconditions.checkNotNull(getWorkFlowService, "getWorkFlowService should not be null");
      Preconditions.checkArgument(
          domain != null && !"".equals(domain), "domain should not be an empty string");

      this.getWorkFlowService = getWorkFlowService;
      this.domain = domain;
    }

    public Worker newWorker(String taskList) {
      return newWorker(taskList, null);
    }

    public Worker newWorker(String taskList, WorkerOptions options) {
      Preconditions.checkArgument(
          taskList != null && !"".equals(taskList), "taskList should not be an empty string");

      synchronized (this) {
        Preconditions.checkState(
            state == State.Initial,
            String.format(
                statusErrorMessage, "create new worker", state.name(), State.Initial.name()));
        Worker worker = new Worker(getWorkFlowService.get(), domain, taskList, options);
        workers.add(worker);
        return worker;
      }
    }

    public void start() {
      synchronized (this) {
        Preconditions.checkState(
            state == State.Initial || state == State.Started,
            String.format(
                statusErrorMessage,
                "start WorkerFactory",
                state.name(),
                String.format("%s, %s", State.Initial.name(), State.Initial.name())));
        if (state == State.Started) {
          return;
        }
        state = State.Started;
      }

      for (Worker worker : workers) {
        worker.start();
      }
    }

    public void shutdown(Duration timeout) {
      synchronized (this) {
        state = State.Shutdown;

        for (Worker worker : workers) {
          worker.shutdown(timeout);
        }
      }
    }

    enum State {
      Initial,
      Started,
      Shutdown
    }
  }
}
