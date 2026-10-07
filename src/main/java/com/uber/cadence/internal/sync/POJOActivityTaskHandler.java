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

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.RateLimiter;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.RespondActivityTaskCompletedRequest;
import com.uber.cadence.RespondActivityTaskFailedRequest;
import com.uber.cadence.activity.ActivityTask;
import com.uber.cadence.client.ActivityCancelledException;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.internal.common.CheckedExceptionWrapper;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.worker.ActivityTaskHandler;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.SimulatedTimeoutException;
import com.uber.m3.tally.Scope;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;

class POJOActivityTaskHandler implements ActivityTaskHandler {
  private static final RateLimiter metricsRateLimiter = RateLimiter.create(1);

  private final DataConverter dataConverter;
  private final ScheduledExecutorService heartbeatExecutor;
  private volatile RegistryInternal registry = RegistryInternal.EMPTY;
  private IWorkflowService service;
  private final String domain;

  POJOActivityTaskHandler(
      IWorkflowService service,
      String domain,
      DataConverter dataConverter,
      ScheduledExecutorService heartbeatExecutor) {
    this.service = service;
    this.domain = domain;
    this.dataConverter = dataConverter;
    this.heartbeatExecutor = heartbeatExecutor;
  }

  private ActivityTaskHandler.Result mapToActivityFailure(
      Throwable failure, Scope metricsScope, boolean isLocalActivity) {

    if (failure instanceof ActivityCancelledException) {
      if (isLocalActivity) {
        metricsScope.counter(MetricsType.LOCAL_ACTIVITY_CANCELED_COUNTER).inc(1);
      }
      throw new CancellationException(failure.getMessage());
    }

    // Only expected during unit tests.
    if (failure instanceof SimulatedTimeoutException) {
      SimulatedTimeoutException timeoutException = (SimulatedTimeoutException) failure;
      failure =
          new SimulatedTimeoutExceptionInternal(
              timeoutException.getTimeoutType(),
              dataConverter.toData(timeoutException.getDetails()));
    }

    if (failure instanceof Error) {
      if (isLocalActivity) {
        metricsScope.counter(MetricsType.LOCAL_ACTIVITY_ERROR_COUNTER).inc(1);
      } else {
        metricsScope.counter(MetricsType.ACTIVITY_TASK_ERROR_COUNTER).inc(1);
      }
      throw (Error) failure;
    }

    if (isLocalActivity) {
      metricsScope.counter(MetricsType.LOCAL_ACTIVITY_FAILED_COUNTER).inc(1);
    } else {
      metricsScope.counter(MetricsType.ACTIVITY_EXEC_FAILED_COUNTER).inc(1);
    }

    RespondActivityTaskFailedRequest result = new RespondActivityTaskFailedRequest();
    failure = CheckedExceptionWrapper.unwrap(failure);
    result.setReason(failure.getClass().getName());
    result.setDetails(dataConverter.toData(failure));
    return new ActivityTaskHandler.Result(null, new Result.TaskFailedResult(result, failure), null);
  }

  @Override
  public boolean isAnyTypeSupported() {
    return registry.hasActivities();
  }

  void setRegistry(RegistryInternal registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  /** Replaces all registered activities with the given activity implementation objects. */
  void setActivitiesImplementation(Object[] activitiesImplementation) {
    setRegistry(RegistryInternal.EMPTY.withActivityImplementations(activitiesImplementation));
  }

  @Override
  public Result handle(
      PollForActivityTaskResponse pollResponse, Scope metricsScope, boolean isLocalActivity) {
    String activityType = pollResponse.getActivityType().getName();
    ActivityTaskImpl activityTask = new ActivityTaskImpl(pollResponse);
    RegistryInternal registry = this.registry;
    ActivityRegistration activity = registry.getActivity(activityType);
    if (activity == null) {
      String knownTypes = Joiner.on(", ").join(registry.getActivityTypes());
      return mapToActivityFailure(
          new IllegalArgumentException(
              "Activity Type \""
                  + activityType
                  + "\" is not registered with a worker. Known types are: "
                  + knownTypes),
          metricsScope,
          isLocalActivity);
    }
    if (metricsRateLimiter.tryAcquire(1)) {
      if (isLocalActivity) {
        metricsScope
            .gauge(MetricsType.LOCAL_ACTIVITY_ACTIVE_THREAD_COUNT)
            .update(Thread.activeCount());
      } else {
        metricsScope.gauge(MetricsType.ACTIVITY_ACTIVE_THREAD_COUNT).update(Thread.activeCount());
      }
    }
    if (isLocalActivity) {
      return executeLocal(activity, activityTask, metricsScope);
    }
    return execute(activity, activityTask, metricsScope);
  }

  private ActivityTaskHandler.Result execute(
      ActivityRegistration activity, ActivityTask task, Scope metricsScope) {
    ActivityExecutionContext context =
        new ActivityExecutionContextImpl(service, domain, task, dataConverter, heartbeatExecutor);
    byte[] input = task.getInput();
    CurrentActivityExecutionContext.set(context);
    try {
      Object[] args = dataConverter.fromDataArray(input, activity.getParameterTypes());
      Object result = activity.invoke(args);
      RespondActivityTaskCompletedRequest request = new RespondActivityTaskCompletedRequest();
      if (context.isDoNotCompleteOnReturn()) {
        return new ActivityTaskHandler.Result(null, null, null);
      }
      if (!activity.isReturnsVoid()) {
        request.setResult(dataConverter.toData(result));
      }
      return new ActivityTaskHandler.Result(request, null, null);
    } catch (Throwable e) {
      return mapToActivityFailure(e, metricsScope, false);
    } finally {
      CurrentActivityExecutionContext.unset();
    }
  }

  private ActivityTaskHandler.Result executeLocal(
      ActivityRegistration activity, ActivityTask task, Scope metricsScope) {
    ActivityExecutionContext context = new LocalActivityExecutionContextImpl(service, domain, task);
    CurrentActivityExecutionContext.set(context);
    byte[] input = task.getInput();
    try {
      Object[] args = dataConverter.fromDataArray(input, activity.getParameterTypes());
      Object result = activity.invoke(args);
      RespondActivityTaskCompletedRequest request = new RespondActivityTaskCompletedRequest();
      if (!activity.isReturnsVoid()) {
        request.setResult(dataConverter.toData(result));
      }
      return new ActivityTaskHandler.Result(request, null, null);
    } catch (Throwable e) {
      return mapToActivityFailure(e, metricsScope, true);
    } finally {
      CurrentActivityExecutionContext.unset();
    }
  }

  // This is only for unit test to mock service and set expectations.
  void setWorkflowService(IWorkflowService service) {
    this.service = service;
  }
}
