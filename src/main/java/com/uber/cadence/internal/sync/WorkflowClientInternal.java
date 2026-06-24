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

package com.uber.cadence.internal.sync;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.uber.cadence.BackfillScheduleRequest;
import com.uber.cadence.CadenceError;
import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.DeleteScheduleRequest;
import com.uber.cadence.DescribeScheduleRequest;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.PauseScheduleRequest;
import com.uber.cadence.RefreshWorkflowTasksRequest;
import com.uber.cadence.UnpauseScheduleRequest;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.BatchRequest;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientInterceptor;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import com.uber.cadence.client.schedule.ListSchedulesResult;
import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import com.uber.cadence.client.schedule.ScheduleDescription;
import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import com.uber.cadence.client.schedule.ScheduleState;
import com.uber.cadence.internal.external.GenericWorkflowClientExternalImpl;
import com.uber.cadence.internal.external.ManualActivityCompletionClientFactory;
import com.uber.cadence.internal.external.ManualActivityCompletionClientFactoryImpl;
import com.uber.cadence.internal.metrics.ClientVersionEmitter;
import com.uber.cadence.internal.sync.WorkflowInvocationHandler.InvocationType;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.QueryMethod;
import com.uber.cadence.workflow.WorkflowMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class WorkflowClientInternal implements WorkflowClient {

  private final GenericWorkflowClientExternalImpl genericClient;
  private final ManualActivityCompletionClientFactory manualActivityCompletionClientFactory;
  private final WorkflowClientInterceptor[] interceptors;
  private final IWorkflowService workflowService;
  private final WorkflowClientOptions clientOptions;
  private static boolean emittingClientVersion = false;

  /**
   * Creates client that connects to an instance of the Cadence Service.
   *
   * @param service client to the Cadence Service endpoint.
   * @param options Options (like {@link com.uber.cadence.converter.DataConverter} override) for
   *     configuring client.
   */
  public static WorkflowClient newInstance(
      IWorkflowService service, WorkflowClientOptions options) {
    Objects.requireNonNull(service);
    Objects.requireNonNull(options);

    emitClientVersion(options);
    return new WorkflowClientInternal(service, options);
  }

  private WorkflowClientInternal(IWorkflowService service, WorkflowClientOptions options) {
    this.clientOptions = options;
    this.workflowService = service;
    this.genericClient =
        new GenericWorkflowClientExternalImpl(
            service, options.getDomain(), options.getMetricsScope());
    this.interceptors = options.getInterceptors();
    this.manualActivityCompletionClientFactory =
        new ManualActivityCompletionClientFactoryImpl(
            service, options.getDomain(), options.getDataConverter(), options.getMetricsScope());
  }

  @Override
  public WorkflowClientOptions getOptions() {
    return clientOptions;
  }

  @Override
  public IWorkflowService getService() {
    return workflowService;
  }

  @Override
  public <T> T newWorkflowStub(Class<T> workflowInterface) {
    return newWorkflowStub(workflowInterface, (WorkflowOptions) null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T newWorkflowStub(Class<T> workflowInterface, WorkflowOptions options) {
    checkAnnotation(workflowInterface, WorkflowMethod.class);
    WorkflowInvocationHandler invocationHandler =
        new WorkflowInvocationHandler(
            workflowInterface, clientOptions, genericClient, options, interceptors);
    return (T)
        Proxy.newProxyInstance(
            workflowInterface.getClassLoader(),
            new Class<?>[] {workflowInterface},
            invocationHandler);
  }

  @SafeVarargs
  private static <T> void checkAnnotation(
      Class<T> workflowInterface, Class<? extends Annotation>... annotationClasses) {
    TypeToken<?>.TypeSet interfaces = TypeToken.of(workflowInterface).getTypes().interfaces();
    if (interfaces.isEmpty()) {
      throw new IllegalArgumentException("Workflow must implement at least one interface");
    }
    for (TypeToken<?> i : interfaces) {
      for (Method method : i.getRawType().getMethods()) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
          Object workflowMethod = method.getAnnotation(annotationClass);
          if (workflowMethod != null) {
            return;
          }
        }
      }
    }
    throw new IllegalArgumentException(
        "Workflow interface "
            + workflowInterface.getName()
            + " doesn't have method annotated with any of "
            + Arrays.toString(annotationClasses));
  }

  @Override
  public <T> T newWorkflowStub(Class<T> workflowInterface, String workflowId) {
    return newWorkflowStub(workflowInterface, workflowId, Optional.empty());
  }

  @Override
  public <T> T newWorkflowStub(
      Class<T> workflowInterface, String workflowId, Optional<String> runId) {
    checkAnnotation(workflowInterface, WorkflowMethod.class, QueryMethod.class);
    if (Strings.isNullOrEmpty(workflowId)) {
      throw new IllegalArgumentException("workflowId is null or empty");
    }
    WorkflowExecution execution = new WorkflowExecution();
    execution.setWorkflowId(workflowId);
    if (runId.isPresent()) {
      execution.setRunId(runId.get());
    }
    WorkflowInvocationHandler invocationHandler =
        new WorkflowInvocationHandler(
            workflowInterface, clientOptions, genericClient, execution, interceptors);
    @SuppressWarnings("unchecked")
    T result =
        (T)
            Proxy.newProxyInstance(
                workflowInterface.getClassLoader(),
                new Class<?>[] {workflowInterface},
                invocationHandler);
    return result;
  }

  @Override
  public WorkflowStub newUntypedWorkflowStub(String workflowType, WorkflowOptions options) {
    WorkflowStub result = new WorkflowStubImpl(clientOptions, genericClient, workflowType, options);
    for (WorkflowClientInterceptor i : interceptors) {
      result = i.newUntypedWorkflowStub(workflowType, options, result);
    }
    return result;
  }

  @Override
  public WorkflowStub newUntypedWorkflowStub(
      String workflowId, Optional<String> runId, Optional<String> workflowType) {
    WorkflowExecution execution = new WorkflowExecution().setWorkflowId(workflowId);
    if (runId.isPresent()) {
      execution.setRunId(runId.get());
    }
    return newUntypedWorkflowStub(execution, workflowType);
  }

  @Override
  public WorkflowStub newUntypedWorkflowStub(
      WorkflowExecution execution, Optional<String> workflowType) {
    return new WorkflowStubImpl(clientOptions, genericClient, workflowType, execution);
  }

  @Override
  public ActivityCompletionClient newActivityCompletionClient() {
    ActivityCompletionClient result =
        new ActivityCompletionClientImpl(manualActivityCompletionClientFactory, () -> {});
    for (WorkflowClientInterceptor i : interceptors) {
      result = i.newActivityCompletionClient(result);
    }
    return result;
  }

  @Override
  public BatchRequest newSignalWithStartRequest() {
    return new SignalWithStartBatchRequest();
  }

  @Override
  public WorkflowExecution signalWithStart(BatchRequest signalWithStartBatch) {
    return ((SignalWithStartBatchRequest) signalWithStartBatch).execute();
  }

  @Override
  public WorkflowExecution enqueueSignalWithStart(BatchRequest signalWithStartBatch) {
    return ((SignalWithStartBatchRequest) signalWithStartBatch).enqueue();
  }

  @Override
  public void refreshWorkflowTasks(RefreshWorkflowTasksRequest refreshWorkflowTasksRequest)
      throws CadenceError {
    workflowService.RefreshWorkflowTasks(refreshWorkflowTasksRequest);
  }

  @Override
  public void createSchedule(
      String scheduleId,
      ScheduleSpec spec,
      ScheduleAction action,
      SchedulePolicies policies,
      ScheduleState state,
      Map<String, Object> memo)
      throws CadenceError {
    String domain = clientOptions.getDomain();
    CreateScheduleRequest req =
        new CreateScheduleRequest()
            .setDomain(domain)
            .setScheduleId(scheduleId)
            .setSpec(spec)
            .setAction(action)
            .setPolicies(policies);
    workflowService.CreateSchedule(req);
  }

  @Override
  public ScheduleDescription describeSchedule(String scheduleId) throws CadenceError {
    String domain = clientOptions.getDomain();
    DescribeScheduleResponse resp =
        workflowService.DescribeSchedule(
            new DescribeScheduleRequest().setDomain(domain).setScheduleId(scheduleId));
    return new ScheduleDescription(
        resp.getSpec(),
        resp.getAction(),
        resp.getPolicies(),
        resp.getState(),
        resp.getInfo(),
        null,
        null);
  }

  @Override
  public void updateSchedule(
      String scheduleId, Function<ScheduleDescription, ScheduleDescription> updater)
      throws CadenceError {
    ScheduleDescription current = describeSchedule(scheduleId);
    ScheduleDescription updated = updater.apply(current);
    String domain = clientOptions.getDomain();
    workflowService.UpdateSchedule(
        new UpdateScheduleRequest()
            .setDomain(domain)
            .setScheduleId(scheduleId)
            .setSpec(updated.getSpec())
            .setAction(updated.getAction())
            .setPolicies(updated.getPolicies()));
  }

  @Override
  public void deleteSchedule(String scheduleId) throws CadenceError {
    String domain = clientOptions.getDomain();
    workflowService.DeleteSchedule(
        new DeleteScheduleRequest().setDomain(domain).setScheduleId(scheduleId));
  }

  @Override
  public void pauseSchedule(String scheduleId, String reason) throws CadenceError {
    String domain = clientOptions.getDomain();
    workflowService.PauseSchedule(
        new PauseScheduleRequest().setDomain(domain).setScheduleId(scheduleId).setReason(reason));
  }

  @Override
  public void unpauseSchedule(String scheduleId, String reason) throws CadenceError {
    unpauseSchedule(scheduleId, reason, null);
  }

  @Override
  public void unpauseSchedule(String scheduleId, String reason, ScheduleCatchUpPolicy catchUpPolicy)
      throws CadenceError {
    String domain = clientOptions.getDomain();
    workflowService.UnpauseSchedule(
        new UnpauseScheduleRequest()
            .setDomain(domain)
            .setScheduleId(scheduleId)
            .setReason(reason)
            .setCatchUpPolicy(catchUpPolicy));
  }

  @Override
  public void backfillSchedule(
      String scheduleId, Instant startTime, Instant endTime, ScheduleOverlapPolicy overlapPolicy)
      throws CadenceError {
    String domain = clientOptions.getDomain();
    workflowService.BackfillSchedule(
        new BackfillScheduleRequest()
            .setDomain(domain)
            .setScheduleId(scheduleId)
            .setStartTime(startTime)
            .setEndTime(endTime)
            .setOverlapPolicy(overlapPolicy));
  }

  @Override
  public ListSchedulesResult listSchedules(int pageSize, byte[] nextPageToken) throws CadenceError {
    String domain = clientOptions.getDomain();
    ListSchedulesResponse resp =
        workflowService.ListSchedules(
            new ListSchedulesRequest()
                .setDomain(domain)
                .setPageSize(pageSize)
                .setNextPageToken(nextPageToken));
    return new ListSchedulesResult(resp.getSchedules(), resp.getNextPageToken());
  }

  public static WorkflowExecution start(Functions.Proc workflow) {
    WorkflowInvocationHandler.initAsyncInvocation(InvocationType.START);
    try {
      workflow.apply();
      return WorkflowInvocationHandler.getAsyncInvocationResult(WorkflowExecution.class);
    } finally {
      WorkflowInvocationHandler.closeAsyncInvocation();
    }
  }

  public static <A1> WorkflowExecution start(Functions.Proc1<A1> workflow, A1 arg1) {
    return start(() -> workflow.apply(arg1));
  }

  public static <A1, A2> WorkflowExecution start(
      Functions.Proc2<A1, A2> workflow, A1 arg1, A2 arg2) {
    return start(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3> WorkflowExecution start(
      Functions.Proc3<A1, A2, A3> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return start(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4> WorkflowExecution start(
      Functions.Proc4<A1, A2, A3, A4> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5> WorkflowExecution start(
      Functions.Proc5<A1, A2, A3, A4, A5> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6> WorkflowExecution start(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  public static <R> WorkflowExecution start(Functions.Func<R> workflow) {
    return start(
        () -> { // Need {} to call start(Proc...)
          workflow.apply();
        });
  }

  public static <A1, R> WorkflowExecution start(Functions.Func1<A1, R> workflow, A1 arg1) {
    return start(() -> workflow.apply(arg1));
  }

  public static <A1, A2, R> WorkflowExecution start(
      Functions.Func2<A1, A2, R> workflow, A1 arg1, A2 arg2) {
    return start(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3, R> WorkflowExecution start(
      Functions.Func3<A1, A2, A3, R> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return start(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4, R> WorkflowExecution start(
      Functions.Func4<A1, A2, A3, A4, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5, R> WorkflowExecution start(
      Functions.Func5<A1, A2, A3, A4, A5, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6, R> WorkflowExecution start(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return start(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  public static WorkflowExecution enqueueStart(Functions.Proc workflow) {
    WorkflowInvocationHandler.initAsyncInvocation(InvocationType.ENQUEUE_START);
    try {
      workflow.apply();
      return WorkflowInvocationHandler.getAsyncInvocationResult(WorkflowExecution.class);
    } finally {
      WorkflowInvocationHandler.closeAsyncInvocation();
    }
  }

  public static <A1> WorkflowExecution enqueueStart(Functions.Proc1<A1> workflow, A1 arg1) {
    return enqueueStart(() -> workflow.apply(arg1));
  }

  public static <A1, A2> WorkflowExecution enqueueStart(
      Functions.Proc2<A1, A2> workflow, A1 arg1, A2 arg2) {
    return enqueueStart(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3> WorkflowExecution enqueueStart(
      Functions.Proc3<A1, A2, A3> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4> WorkflowExecution enqueueStart(
      Functions.Proc4<A1, A2, A3, A4> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5> WorkflowExecution enqueueStart(
      Functions.Proc5<A1, A2, A3, A4, A5> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6> WorkflowExecution enqueueStart(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  public static <R> WorkflowExecution enqueueStart(Functions.Func<R> workflow) {
    return enqueueStart(
        () -> {
          workflow.apply();
        });
  }

  public static <A1, R> WorkflowExecution enqueueStart(Functions.Func1<A1, R> workflow, A1 arg1) {
    return enqueueStart(() -> workflow.apply(arg1));
  }

  public static <A1, A2, R> WorkflowExecution enqueueStart(
      Functions.Func2<A1, A2, R> workflow, A1 arg1, A2 arg2) {
    return enqueueStart(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3, R> WorkflowExecution enqueueStart(
      Functions.Func3<A1, A2, A3, R> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4, R> WorkflowExecution enqueueStart(
      Functions.Func4<A1, A2, A3, A4, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5, R> WorkflowExecution enqueueStart(
      Functions.Func5<A1, A2, A3, A4, A5, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6, R> WorkflowExecution enqueueStart(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return enqueueStart(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  @SuppressWarnings("unchecked")
  public static CompletableFuture<Void> execute(Functions.Proc workflow) {
    WorkflowInvocationHandler.initAsyncInvocation(InvocationType.EXECUTE);
    try {
      workflow.apply();
      return WorkflowInvocationHandler.getAsyncInvocationResult(CompletableFuture.class);
    } finally {
      WorkflowInvocationHandler.closeAsyncInvocation();
    }
  }

  public static <A1> CompletableFuture<Void> execute(Functions.Proc1<A1> workflow, A1 arg1) {
    return execute(() -> workflow.apply(arg1));
  }

  public static <A1, A2> CompletableFuture<Void> execute(
      Functions.Proc2<A1, A2> workflow, A1 arg1, A2 arg2) {
    return execute(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3> CompletableFuture<Void> execute(
      Functions.Proc3<A1, A2, A3> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return execute(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4> CompletableFuture<Void> execute(
      Functions.Proc4<A1, A2, A3, A4> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5> CompletableFuture<Void> execute(
      Functions.Proc5<A1, A2, A3, A4, A5> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6> CompletableFuture<Void> execute(
      Functions.Proc6<A1, A2, A3, A4, A5, A6> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  @SuppressWarnings("unchecked")
  public static <R> CompletableFuture<R> execute(Functions.Func<R> workflow) {
    return (CompletableFuture<R>)
        execute(
            () -> {
              // Need {} to call execute(Proc...)
              workflow.apply();
            });
  }

  public static <A1, R> CompletableFuture<R> execute(Functions.Func1<A1, R> workflow, A1 arg1) {
    return execute(() -> workflow.apply(arg1));
  }

  public static <A1, A2, R> CompletableFuture<R> execute(
      Functions.Func2<A1, A2, R> workflow, A1 arg1, A2 arg2) {
    return execute(() -> workflow.apply(arg1, arg2));
  }

  public static <A1, A2, A3, R> CompletableFuture<R> execute(
      Functions.Func3<A1, A2, A3, R> workflow, A1 arg1, A2 arg2, A3 arg3) {
    return execute(() -> workflow.apply(arg1, arg2, arg3));
  }

  public static <A1, A2, A3, A4, R> CompletableFuture<R> execute(
      Functions.Func4<A1, A2, A3, A4, R> workflow, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4));
  }

  public static <A1, A2, A3, A4, A5, R> CompletableFuture<R> execute(
      Functions.Func5<A1, A2, A3, A4, A5, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5));
  }

  public static <A1, A2, A3, A4, A5, A6, R> CompletableFuture<R> execute(
      Functions.Func6<A1, A2, A3, A4, A5, A6, R> workflow,
      A1 arg1,
      A2 arg2,
      A3 arg3,
      A4 arg4,
      A5 arg5,
      A6 arg6) {
    return execute(() -> workflow.apply(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  private static synchronized void emitClientVersion(WorkflowClientOptions options) {
    if (emittingClientVersion) {
      return;
    }

    emittingClientVersion = true;
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(
            new ClientVersionEmitter(options.getMetricsScope(), options.getDomain()),
            30,
            60,
            TimeUnit.SECONDS);
  }
}
