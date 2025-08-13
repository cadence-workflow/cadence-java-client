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

package com.uber.cadence.internal.worker;

import com.uber.cadence.Header;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCanceledRequest;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.context.ContextPropagator;
import com.uber.cadence.internal.common.RpcRetryer;
import com.uber.cadence.internal.logging.LoggerTag;
import com.uber.cadence.internal.metrics.MetricsTag;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.tracing.TracingPropagator;
import com.uber.cadence.internal.worker.ActivityTaskHandler.Result;
import com.uber.cadence.internal.worker.Poller.PollTask;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.Stopwatch;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TException;
import org.slf4j.MDC;

public class ActivityWorker extends SuspendableWorkerBase {

  protected final SingleWorkerOptions options;
  private final ActivityTaskHandler handler;
  private final IWorkflowService service;
  private final String domain;
  private final String taskList;
  private final Tracer tracer;
  private final TracingPropagator spanFactory;

  public ActivityWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler) {
    this(service, domain, taskList, options, handler, "Activity Poller taskList=");
  }

  public ActivityWorker(
      IWorkflowService service,
      String domain,
      String taskList,
      SingleWorkerOptions options,
      ActivityTaskHandler handler,
      String pollThreadNamePrefix) {
    this.service = Objects.requireNonNull(service);
    this.domain = Objects.requireNonNull(domain);
    this.taskList = Objects.requireNonNull(taskList);
    this.handler = handler;
    this.tracer = options.getTracer();
    this.spanFactory = new TracingPropagator(options.getTracer());

    PollerOptions pollerOptions = options.getPollerOptions();
    if (pollerOptions.getPollThreadNamePrefix() == null) {
      pollerOptions =
          PollerOptions.newBuilder(pollerOptions)
              .setPollThreadNamePrefix(
                  pollThreadNamePrefix + "\"" + taskList + "\", domain=\"" + domain + "\"")
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
              getOrCreateActivityPollTask(),
              new PollTaskExecutor<>(domain, taskList, options, new TaskHandlerImpl(handler)),
              options.getPollerOptions(),
              options.getMetricsScope(),
              options.getExecutorWrapper());
      poller.start();
      setPoller(poller);
      options.getMetricsScope().counter(MetricsType.WORKER_START_COUNTER).inc(1);
    }
  }

  protected PollTask<PollForActivityTaskResponse> getOrCreateActivityPollTask() {
    return new ActivityPollTask(service, domain, taskList, options);
  }

  private class TaskHandlerImpl
      implements PollTaskExecutor.TaskHandler<PollForActivityTaskResponse> {

    final ActivityTaskHandler handler;

    private TaskHandlerImpl(ActivityTaskHandler handler) {
      this.handler = handler;
    }

    @Override
    public void handle(PollForActivityTaskResponse task) throws Exception {
      Scope metricsScope =
          options
              .getMetricsScope()
              .tagged(
                  ImmutableMap.of(
                      MetricsTag.ACTIVITY_TYPE,
                      task.getActivityType().getName(),
                      MetricsTag.WORKFLOW_TYPE,
                      task.getWorkflowType().getName()));

      metricsScope
          .timer(MetricsType.ACTIVITY_SCHEDULED_TO_START_LATENCY)
          .record(
              Duration.ofNanos(
                  task.getStartedTimestamp() - task.getScheduledTimestampOfThisAttempt()));

      // The following tags are for logging.
      MDC.put(LoggerTag.ACTIVITY_ID, task.getActivityId());
      MDC.put(LoggerTag.ACTIVITY_TYPE, task.getActivityType().getName());
      MDC.put(LoggerTag.WORKFLOW_ID, task.getWorkflowExecution().getWorkflowId());
      MDC.put(LoggerTag.RUN_ID, task.getWorkflowExecution().getRunId());
      MDC.put(LoggerTag.ATTEMPT, String.valueOf(task.getAttempt()));

      propagateContext(task);
      Span span = spanFactory.spanForExecuteActivity(task);
      try (io.opentracing.Scope scope = tracer.activateSpan(span)) {
        Stopwatch sw = metricsScope.timer(MetricsType.ACTIVITY_EXEC_LATENCY).start();
        ActivityTaskHandler.Result response = handler.handle(task, metricsScope, false);
        sw.stop();

        sw = metricsScope.timer(MetricsType.ACTIVITY_RESP_LATENCY).start();
        sendReply(task, response, metricsScope);
        sw.stop();

        long nanoTime =
            TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        Duration duration = Duration.ofNanos(nanoTime - task.getScheduledTimestampOfThisAttempt());
        metricsScope.timer(MetricsType.ACTIVITY_E2E_LATENCY).record(duration);

      } catch (CancellationException e) {
        RespondActivityTaskCanceledRequest cancelledRequest =
            new RespondActivityTaskCanceledRequest();
        cancelledRequest.setDetails(
            String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
        Stopwatch sw = metricsScope.timer(MetricsType.ACTIVITY_RESP_LATENCY).start();
        sendReply(task, new Result(null, null, cancelledRequest), metricsScope);
        sw.stop();
      } finally {
        span.finish();
        MDC.remove(LoggerTag.ACTIVITY_ID);
        MDC.remove(LoggerTag.ACTIVITY_TYPE);
        MDC.remove(LoggerTag.WORKFLOW_ID);
        MDC.remove(LoggerTag.RUN_ID);
        MDC.remove(LoggerTag.ATTEMPT);
        unsetCurrentContext();
      }
    }

    void propagateContext(PollForActivityTaskResponse response) {
      if (options.getContextPropagators() == null || options.getContextPropagators().isEmpty()) {
        return;
      }

      Header headers = response.getHeader();
      if (headers == null) {
        return;
      }

      Map<String, byte[]> headerData = new HashMap<>();
      headers
          .getFields()
          .forEach(
              (k, v) -> {
                headerData.put(k, org.apache.thrift.TBaseHelper.byteBufferToByteArray(v));
              });

      for (ContextPropagator propagator : options.getContextPropagators()) {
        propagator.setCurrentContext(propagator.deserializeContext(headerData));
      }
    }

    void unsetCurrentContext() {
      for (ContextPropagator propagator : options.getContextPropagators()) {
        propagator.unsetCurrentContext();
      }
    }

    @Override
    public Throwable wrapFailure(PollForActivityTaskResponse task, Throwable failure) {
      WorkflowExecution execution = task.getWorkflowExecution();
      return new RuntimeException(
          "Failure processing activity task. WorkflowID="
              + execution.getWorkflowId()
              + ", RunID="
              + execution.getRunId()
              + ", ActivityType="
              + task.getActivityType().getName()
              + ", ActivityID="
              + task.getActivityId(),
          failure);
    }

    private void sendReply(
        PollForActivityTaskResponse task, ActivityTaskHandler.Result response, Scope metricsScope)
        throws TException {
      RespondActivityTaskCompletedRequest taskCompleted = response.getTaskCompleted();
      if (taskCompleted != null) {
        taskCompleted.setTaskToken(task.getTaskToken());
        taskCompleted.setIdentity(options.getIdentity());
        RpcRetryer.retry(() -> service.RespondActivityTaskCompleted(taskCompleted));
        metricsScope.counter(MetricsType.ACTIVITY_TASK_COMPLETED_COUNTER).inc(1);
      } else {
        if (response.getTaskFailedResult() != null) {
          RespondActivityTaskFailedRequest taskFailed =
              response.getTaskFailedResult().getTaskFailedRequest();
          taskFailed.setTaskToken(task.getTaskToken());
          taskFailed.setIdentity(options.getIdentity());
          RpcRetryer.retry(() -> service.RespondActivityTaskFailed(taskFailed));
          metricsScope.counter(MetricsType.ACTIVITY_TASK_FAILED_COUNTER).inc(1);
        } else {
          RespondActivityTaskCanceledRequest taskCancelled = response.getTaskCancelled();
          if (taskCancelled != null) {
            taskCancelled.setTaskToken(task.getTaskToken());
            taskCancelled.setIdentity(options.getIdentity());
            RpcRetryer.retry(() -> service.RespondActivityTaskCanceled(taskCancelled));
            metricsScope.counter(MetricsType.ACTIVITY_TASK_CANCELED_COUNTER).inc(1);
          }
        }
      }
      // Manual activity completion
    }
  }
}
