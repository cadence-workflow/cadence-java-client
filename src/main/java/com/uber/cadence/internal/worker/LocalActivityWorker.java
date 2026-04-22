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

package com.uber.cadence.internal.worker;

import com.uber.cadence.EventType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.MarkerRecordedEventAttributes;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.internal.common.LocalActivityMarkerData;
import com.uber.cadence.internal.metrics.HistogramBuckets;
import com.uber.cadence.internal.metrics.MetricsEmit;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.replay.ClockDecisionContext;
import com.uber.cadence.internal.replay.ExecuteLocalActivityParameters;
import com.uber.cadence.internal.tracing.TracingPropagator;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class LocalActivityWorker extends SuspendableWorkerBase {

  private static final String POLL_THREAD_NAME_PREFIX = "Local Activity Poller taskList=";

  private final ActivityTaskHandler handler;
  private final String domain;
  private final String taskList;
  private final SingleWorkerOptions options;
  private final LocalActivityPollTask laPollTask;
  private final TracingPropagator spanFactory;
  private final Tracer tracer;

  public LocalActivityWorker(
      String domain, String taskList, SingleWorkerOptions options, ActivityTaskHandler handler) {
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.handler = handler;
    this.laPollTask = new LocalActivityPollTask();
    this.spanFactory = new TracingPropagator(options.getTracer());
    this.tracer = options.getTracer();

    PollerOptions pollerOptions = options.getPollerOptions();
    if (pollerOptions.getPollThreadNamePrefix() == null) {
      pollerOptions =
          PollerOptions.newBuilder(pollerOptions)
              .setPollThreadNamePrefix(
                  POLL_THREAD_NAME_PREFIX + "\"" + taskList + "\", domain=\"" + domain + "\"")
              .build();
    }
    this.options = SingleWorkerOptions.newBuilder(options).setPollerOptions(pollerOptions).build();
  }

  @Override
  public void start() {
    if (handler.isAnyTypeSupported()) {
      SuspendableWorker poller =
          new Poller<>(
              options.getIdentity(),
              laPollTask,
              new PollTaskExecutor<>(domain, taskList, options, new TaskHandlerImpl(handler)),
              options.getPollerOptions(),
              options.getMetricsScope(),
              options.getExecutorWrapper());
      poller.start();
      setPoller(poller);
      options.getMetricsScope().counter(MetricsType.WORKER_START_COUNTER).inc(1);
    }
  }

  public static class Task {
    private final ExecuteLocalActivityParameters params;
    private final Consumer<HistoryEvent> eventConsumer;
    private final LongSupplier currentTimeMillis;
    private final LongSupplier replayTimeUpdatedAtMillis;
    long taskStartTime;
    private final int decisionTimeoutSeconds;

    public Task(
        ExecuteLocalActivityParameters params,
        Consumer<HistoryEvent> eventConsumer,
        int decisionTimeoutSeconds,
        LongSupplier currentTimeMillis,
        LongSupplier replayTimeUpdatedAtMillis) {
      this.params = params;
      this.eventConsumer = eventConsumer;
      this.currentTimeMillis = currentTimeMillis;
      this.replayTimeUpdatedAtMillis = replayTimeUpdatedAtMillis;
      this.decisionTimeoutSeconds = decisionTimeoutSeconds;
    }

    public ExecuteLocalActivityParameters getExecuteLocalActivityParameters() {
      return params;
    }
  }

  public BiFunction<Task, Duration, Boolean> getLocalActivityTaskPoller() {
    return laPollTask;
  }

  private class TaskHandlerImpl implements PollTaskExecutor.TaskHandler<Task> {

    final ActivityTaskHandler handler;

    private TaskHandlerImpl(ActivityTaskHandler handler) {
      this.handler = handler;
    }

    @Override
    public void handle(Task task) throws Exception {
      propagateContext(task.params);

      // start and activate span for local activities
      Span span = spanFactory.spanForExecuteLocalActivity(task);
      try (io.opentracing.Scope scope = tracer.activateSpan(span)) {
        task.taskStartTime = System.currentTimeMillis();
        ActivityTaskHandler.Result result = handleLocalActivity(task);

        LocalActivityMarkerData.Builder markerBuilder = new LocalActivityMarkerData.Builder();
        markerBuilder.setActivityId(task.params.getActivityId());
        markerBuilder.setActivityType(task.params.getActivityType());
        long replayTimeMillis =
            task.currentTimeMillis.getAsLong()
                + (System.currentTimeMillis() - task.replayTimeUpdatedAtMillis.getAsLong());
        markerBuilder.setReplayTimeMillis(replayTimeMillis);

        if (result.getTaskCompleted() != null) {
          markerBuilder.setResult(result.getTaskCompleted().getResult());
        } else if (result.getTaskFailedResult() != null) {
          markerBuilder.setTaskFailedRequest(result.getTaskFailedResult().getTaskFailedRequest());
          markerBuilder.setAttempt(result.getAttempt());
          markerBuilder.setBackoff(result.getBackoff());
        } else {
          markerBuilder.setTaskCancelledRequest(result.getTaskCancelled());
        }

        LocalActivityMarkerData marker = markerBuilder.build();

        HistoryEvent event = new HistoryEvent();
        event.setEventType(EventType.MarkerRecorded);
        MarkerRecordedEventAttributes attributes =
            new MarkerRecordedEventAttributes()
                .setMarkerName(ClockDecisionContext.LOCAL_ACTIVITY_MARKER_NAME)
                .setHeader(marker.getHeader(options.getDataConverter()))
                .setDetails(marker.getResult());
        event.setMarkerRecordedEventAttributes(attributes);
        task.eventConsumer.accept(event);
      } finally {
        span.finish();
        unsetCurrentContext();
      }
    }

    @Override
    public Throwable wrapFailure(Task task, Throwable failure) {
      return new RuntimeException("Failure processing local activity task.", failure);
    }

    private ActivityTaskHandler.Result handleLocalActivity(Task task) throws InterruptedException {
      Map<String, String> activityTypeTag =
          new ImmutableMap.Builder<String, String>(1)
              .put(MetricsTag.ACTIVITY_TYPE, task.params.getActivityType().getName())
              .build();

      Scope metricsScope = options.getMetricsScope().tagged(activityTypeTag);
      metricsScope.counter(MetricsType.LOCAL_ACTIVITY_TOTAL_COUNTER).inc(1);

      PollForActivityTaskResponse pollTask = new PollForActivityTaskResponse();
      pollTask.setWorkflowDomain(task.params.getWorkflowDomain());
      pollTask.setActivityId(task.params.getActivityId());
      pollTask.setWorkflowExecution(task.params.getWorkflowExecution());
      pollTask.setScheduledTimestamp(System.currentTimeMillis());
      pollTask.setStartedTimestamp(System.currentTimeMillis());
      pollTask.setActivityType(task.params.getActivityType());
      pollTask.setInput(task.params.getInput());
      pollTask.setAttempt(task.params.getAttempt());

      MetricsEmit.DualStopwatch sw =
          MetricsEmit.startLatency(
              metricsScope,
              MetricsType.LOCAL_ACTIVITY_EXECUTION_LATENCY,
              HistogramBuckets.DEFAULT_1MS_100S);
      ActivityTaskHandler.Result result = handler.handle(pollTask, metricsScope, true);
      sw.stop();
      result.setAttempt(task.params.getAttempt());

      if (result.getTaskCompleted() != null
          || result.getTaskCancelled() != null
          || task.params.getRetryOptions() == null) {
        return result;
      }

      RetryOptions retryOptions = task.params.getRetryOptions();
      long sleepMillis = retryOptions.calculateSleepTime(task.params.getAttempt());
      long elapsedTask = System.currentTimeMillis() - task.taskStartTime;
      long elapsedTotal = elapsedTask + task.params.getElapsedTime();
      if (retryOptions.shouldRethrow(
          result.getTaskFailedResult().getFailure(),
          task.params.getAttempt(),
          elapsedTotal,
          sleepMillis)) {
        return result;
      } else {
        result.setBackoff(Duration.ofMillis(sleepMillis));
      }

      // For small backoff we do local retry. Otherwise we will schedule timer on server side.
      if (elapsedTask + sleepMillis < task.decisionTimeoutSeconds * 1000) {
        Thread.sleep(sleepMillis);
        task.params.setAttempt(task.params.getAttempt() + 1);
        return handleLocalActivity(task);
      } else {
        return result;
      }
    }
  }

  private void propagateContext(ExecuteLocalActivityParameters params) {
    if (options.getContextPropagators() == null || options.getContextPropagators().isEmpty()) {
      return;
    }

    Optional.ofNullable(params.getContext())
        .filter(context -> !context.isEmpty())
        .ifPresent(this::restoreContext);
  }

  private void unsetCurrentContext() {
    options.getContextPropagators().forEach(ContextPropagator::unsetCurrentContext);
  }

  private void restoreContext(Map<String, byte[]> context) {
    options
        .getContextPropagators()
        .forEach(
            propagator -> propagator.setCurrentContext(propagator.deserializeContext(context)));
  }
}
