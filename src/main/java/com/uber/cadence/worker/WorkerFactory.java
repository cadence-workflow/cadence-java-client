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
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.TaskListKind;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.replay.DeciderCache;
import com.uber.cadence.internal.worker.*;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maintains worker creation and lifecycle. */
public final class WorkerFactory {

  public static WorkerFactory newInstance(WorkflowClient workflowClient) {
    return WorkerFactory.newInstance(workflowClient, WorkerFactoryOptions.defaultInstance());
  }

  public static WorkerFactory newInstance(
      WorkflowClient workflowClient, WorkerFactoryOptions options) {
    WorkerShutDownHandler.registerHandler();
    WorkerFactory workerFactory = new WorkerFactory(workflowClient, options);
    WorkerShutDownHandler.registerWorkerFactory(workerFactory);
    return workerFactory;
  }

  private final List<Worker> workers = new ArrayList<>();
  private final WorkflowClient workflowClient;
  // Guarantee uniqueness for stickyTaskListName when multiple factories
  private final UUID stickyTasklistRandomId = UUID.randomUUID();
  private final ThreadPoolExecutor workflowThreadPool;
  private final AtomicInteger workflowThreadCounter = new AtomicInteger();
  private final WorkerFactoryOptions factoryOptions;

  private Poller<PollForDecisionTaskResponse> stickyPoller;
  private PollDecisionTaskDispatcher dispatcher;
  private DeciderCache cache;

  private State state = State.Initial;

  private final String statusErrorMessage =
      "attempted to %s while in %s state. Acceptable States: %s";
  private static final Logger log = LoggerFactory.getLogger(WorkerFactory.class);
  private static final String STICKY_TASK_LIST_PREFIX = "sticky";
  private static final String STICKY_TASK_LIST_METRIC_TAG = "__" + STICKY_TASK_LIST_PREFIX + "__";
  private static final String POLL_THREAD_NAME = "Sticky Task Poller";

  /**
   * Creates a factory. Workers will be connect to the cadence-server using the workflowService
   * client passed in.
   *
   * @param workflowClient client to the Cadence Service endpoint.
   * @param factoryOptions Options used to configure factory settings
   */
  public WorkerFactory(WorkflowClient workflowClient, WorkerFactoryOptions factoryOptions) {
    this.workflowClient = Objects.requireNonNull(workflowClient);
    this.factoryOptions =
        MoreObjects.firstNonNull(factoryOptions, WorkerFactoryOptions.defaultInstance());

    workflowThreadPool =
        factoryOptions
            .getExecutorWrapper()
            .wrap(
                new ThreadPoolExecutor(
                    0,
                    this.factoryOptions.getMaxWorkflowThreadCount(),
                    1,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>()));
    workflowThreadPool.setThreadFactory(
        r -> new Thread(r, "workflow-thread-" + workflowThreadCounter.incrementAndGet()));

    if (this.factoryOptions.isDisableStickyExecution()) {
      return;
    }

    // initialize the JsonDataConverter with the metrics scope
    JsonDataConverter.setMetricsScope(workflowClient.getOptions().getMetricsScope());

    Scope stickyScope =
        workflowClient
            .getOptions()
            .getMetricsScope()
            .tagged(
                ImmutableMap.of(
                    MetricsTag.DOMAIN,
                    workflowClient.getOptions().getDomain(),
                    MetricsTag.TASK_LIST,
                    STICKY_TASK_LIST_METRIC_TAG));

    this.cache = new DeciderCache(this.factoryOptions.getCacheMaximumSize(), stickyScope);
    dispatcher = new PollDecisionTaskDispatcher(workflowClient.getService());
    stickyPoller =
        new Poller<>(
            workflowClient.getOptions().getIdentity(),
            new WorkflowPollTaskFactory(
                    workflowClient.getService(),
                    workflowClient.getOptions().getDomain(),
                    getStickyTaskListName(),
                    TaskListKind.STICKY,
                    stickyScope,
                    workflowClient.getOptions().getIdentity())
                .get(),
            dispatcher,
            PollerOptions.newBuilder()
                .setPollThreadNamePrefix(POLL_THREAD_NAME)
                .setPollThreadCount(this.factoryOptions.getStickyPollerCount())
                .build(),
            stickyScope,
            factoryOptions.getExecutorWrapper());
  }

  /**
   * Creates worker that connects to an instance of the Cadence Service. It uses the domain
   * configured at the Factory level. New workers cannot be created after the start() has been
   * called
   *
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @return Worker
   */
  public Worker newWorker(String taskList) {
    return newWorker(taskList, null);
  }

  /**
   * Creates worker that connects to an instance of the Cadence Service. It uses the domain
   * configured at the Factory level. New workers cannot be created after the start() has been
   * called
   *
   * @param taskList task list name worker uses to poll. It uses this name for both decision and
   *     activity task list polls.
   * @param options Options (like {@link DataConverter} override) for configuring worker.
   * @return Worker
   */
  public synchronized Worker newWorker(String taskList, WorkerOptions options) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(taskList), "taskList should not be an empty string");
    Preconditions.checkState(
        state == State.Initial,
        String.format(statusErrorMessage, "create new worker", state.name(), State.Initial.name()));

    Worker worker =
        new Worker(
            workflowClient,
            taskList,
            factoryOptions,
            options,
            cache,
            getStickyTaskListName(),
            factoryOptions.getStickyTaskScheduleToStartTimeout(),
            workflowThreadPool,
            workflowClient.getOptions().getContextPropagators());
    workers.add(worker);

    if (!this.factoryOptions.isDisableStickyExecution()) {
      dispatcher.subscribe(taskList, worker.getWorkflowWorker());
    }
    return worker;
  }

  /** Starts all the workers created by this factory. */
  public synchronized void start() {
    Preconditions.checkState(
        state == State.Initial || state == State.Started,
        String.format(
            statusErrorMessage,
            "start WorkerFactory",
            state.name(),
            String.format("%s, %s", State.Initial.name(), State.Started.name())));
    if (state == State.Started) {
      return;
    }
    state = State.Started;

    for (Worker worker : workers) {
      worker.start();
    }

    if (stickyPoller != null) {
      stickyPoller.start();
    }
  }

  /** Was {@link #start()} called. */
  public synchronized boolean isStarted() {
    return state != State.Initial;
  }

  /** Was {@link #shutdown()} or {@link #shutdownNow()} called. */
  public synchronized boolean isShutdown() {
    return state == State.Shutdown;
  }

  /**
   * Returns true if all tasks have completed following shut down. Note that isTerminated is never
   * true unless either shutdown or shutdownNow was called first.
   */
  public synchronized boolean isTerminated() {
    if (state != State.Shutdown) {
      return false;
    }
    if (stickyPoller != null) {
      if (!stickyPoller.isTerminated()) {
        return false;
      }
    }
    for (Worker worker : workers) {
      if (!worker.isTerminated()) {
        return false;
      }
    }
    return true;
  }

  /** @return instance of the Cadence client that this worker uses. */
  public WorkflowClient getWorkflowClient() {
    return workflowClient;
  }

  /**
   * Initiates an orderly shutdown in which polls are stopped and already received decision and
   * activity tasks are executed. After the shutdown calls to {@link
   * com.uber.cadence.activity.Activity#heartbeat(Object)} start throwing {@link
   * com.uber.cadence.client.ActivityWorkerShutdownException}. Invocation has no additional effect
   * if already shut down. This method does not wait for previously received tasks to complete
   * execution. Use {@link #awaitTermination(long, TimeUnit)} to do that.
   */
  public synchronized void shutdown() {
    log.info("shutdown");
    state = State.Shutdown;
    if (stickyPoller != null) {
      stickyPoller.shutdown();
      // To ensure that it doesn't get new tasks before workers are shutdown.
      stickyPoller.awaitTermination(1, TimeUnit.SECONDS);
    }
    for (Worker worker : workers) {
      worker.shutdown();
    }
  }

  /**
   * Initiates an orderly shutdown in which polls are stopped and already received decision and
   * activity tasks are attempted to be stopped. This implementation cancels tasks via
   * Thread.interrupt(), so any task that fails to respond to interrupts may never terminate. Also
   * after the shutdownNow calls to {@link com.uber.cadence.activity.Activity#heartbeat(Object)}
   * start throwing {@link com.uber.cadence.client.ActivityWorkerShutdownException}. Invocation has
   * no additional effect if already shut down. This method does not wait for previously received
   * tasks to complete execution. Use {@link #awaitTermination(long, TimeUnit)} to do that.
   */
  public synchronized void shutdownNow() {
    log.info("shutdownNow");
    state = State.Shutdown;
    if (stickyPoller != null) {
      stickyPoller.shutdownNow();
      // To ensure that it doesn't get new tasks before workers are shutdown.
      stickyPoller.awaitTermination(1, TimeUnit.SECONDS);
    }
    for (Worker worker : workers) {
      worker.shutdownNow();
    }
  }

  /**
   * Checks if we have a valid connection to the Cadence cluster, and potentially resets the peer
   * list
   */
  public CompletableFuture<Boolean> isHealthy() {
    List<CompletableFuture<Boolean>> healthyList =
        workers.stream().map(Worker::isHealthy).collect(Collectors.toList());
    CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> true);
    for (CompletableFuture<Boolean> future : healthyList) {
      result = result.thenCombine(future, (current, other) -> current && other);
    }
    return result;
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request, or the timeout
   * occurs, or the current thread is interrupted, whichever happens first.
   */
  public void awaitTermination(long timeout, TimeUnit unit) {
    log.debug("awaitTermination begin");
    long timeoutMillis = unit.toMillis(timeout);
    timeoutMillis = InternalUtils.awaitTermination(stickyPoller, timeoutMillis);
    for (Worker worker : workers) {
      long t = timeoutMillis; // closure needs immutable value
      timeoutMillis =
          InternalUtils.awaitTermination(
              timeoutMillis, () -> worker.awaitTermination(t, TimeUnit.MILLISECONDS));
    }
    log.debug("awaitTermination done");
  }

  @VisibleForTesting
  DeciderCache getCache() {
    return this.cache;
  }

  private String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "UnknownHost";
    }
  }

  @VisibleForTesting
  String getStickyTaskListName() {
    return this.factoryOptions.isDisableStickyExecution()
        ? null
        : String.format("%s:%s:%s", STICKY_TASK_LIST_PREFIX, getHostName(), stickyTasklistRandomId);
  }

  public synchronized void suspendPolling() {
    if (state != State.Started) {
      return;
    }

    log.info("suspendPolling");
    state = State.Suspended;
    if (stickyPoller != null) {
      stickyPoller.suspendPolling();
    }
    for (Worker worker : workers) {
      worker.suspendPolling();
    }
  }

  public synchronized void resumePolling() {
    if (state != State.Suspended) {
      return;
    }

    log.info("resumePolling");
    state = State.Started;
    if (stickyPoller != null) {
      stickyPoller.resumePolling();
    }
    for (Worker worker : workers) {
      worker.resumePolling();
    }
  }

  enum State {
    Initial,
    Started,
    Suspended,
    Shutdown
  }
}
