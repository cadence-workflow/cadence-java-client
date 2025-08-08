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

import com.uber.cadence.*;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.QueryOptions;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientInterceptor;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.client.WorkflowStub;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import com.uber.cadence.serviceclient.AsyncMethodCallback;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerFactory;
import com.uber.cadence.worker.WorkerOptions;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class TestWorkflowEnvironmentInternal implements TestWorkflowEnvironment {

  private final TestEnvironmentOptions testEnvironmentOptions;
  private final WorkflowServiceWrapper service;
  private final WorkerFactory workerFactory;

  public TestWorkflowEnvironmentInternal(
      WorkflowServiceWrapper workflowServiceWrapper, TestEnvironmentOptions options) {
    if (options == null) {
      this.testEnvironmentOptions = new TestEnvironmentOptions.Builder().build();
    } else {
      this.testEnvironmentOptions = options;
    }

    if (workflowServiceWrapper == null) {
      this.service = new WorkflowServiceWrapper();
    } else {
      this.service = workflowServiceWrapper;
    }

    this.service.lockTimeSkipping("TestWorkflowEnvironmentInternal constructor");
    WorkflowClient client =
        WorkflowClient.newInstance(this.service, testEnvironmentOptions.getWorkflowClientOptions());
    workerFactory =
        WorkerFactory.newInstance(client, testEnvironmentOptions.getWorkerFactoryOptions());
  }

  @Override
  public Worker newWorker(String taskList) {
    return newWorker(taskList, x -> x);
  }

  @Override
  public Worker newWorker(
      String taskList, Function<WorkerOptions.Builder, WorkerOptions.Builder> overrideOptions) {
    WorkerOptions.Builder builder =
        WorkerOptions.newBuilder()
            .setInterceptorFactory(testEnvironmentOptions.getInterceptorFactory());
    builder = overrideOptions.apply(builder);
    return workerFactory.newWorker(taskList, builder.build());
  }

  @Override
  public WorkflowClient newWorkflowClient() {
    WorkflowClientOptions options =
        WorkflowClientOptions.newBuilder()
            .setDataConverter(testEnvironmentOptions.getDataConverter())
            .setInterceptors(new TimeLockingInterceptor(service))
            .setMetricsScope(testEnvironmentOptions.getWorkflowClientOptions().getMetricsScope())
            .setDomain(testEnvironmentOptions.getWorkflowClientOptions().getDomain())
            .build();
    return WorkflowClientInternal.newInstance(service, options);
  }

  @Override
  public WorkflowClient newWorkflowClient(WorkflowClientOptions options) {
    WorkflowClientInterceptor[] existingInterceptors = options.getInterceptors();
    WorkflowClientInterceptor[] interceptors =
        new WorkflowClientInterceptor[existingInterceptors.length + 1];
    System.arraycopy(existingInterceptors, 0, interceptors, 0, existingInterceptors.length);
    interceptors[interceptors.length - 1] = new TimeLockingInterceptor(service);
    WorkflowClientOptions newOptions =
        WorkflowClientOptions.newBuilder(options).setInterceptors(interceptors).build();
    return WorkflowClientInternal.newInstance(service, newOptions);
  }

  @Override
  public long currentTimeMillis() {
    return service.currentTimeMillis();
  }

  @Override
  public void sleep(Duration duration) {
    service.sleep(duration);
  }

  @Override
  public void registerDelayedCallback(Duration delay, Runnable r) {
    service.registerDelayedCallback(delay, r);
  }

  @Override
  public IWorkflowService getWorkflowService() {
    return service;
  }

  @Override
  public String getDomain() {
    return testEnvironmentOptions.getWorkflowClientOptions().getDomain();
  }

  @Override
  public String getDiagnostics() {
    StringBuilder result = new StringBuilder();
    service.getDiagnostics(result);
    return result.toString();
  }

  @Override
  public void close() {
    workerFactory.shutdownNow();
    workerFactory.awaitTermination(10, TimeUnit.SECONDS);
    service.close();
  }

  @Override
  public void start() {
    workerFactory.start();
  }

  @Override
  public boolean isStarted() {
    return workerFactory.isStarted();
  }

  @Override
  public boolean isShutdown() {
    return workerFactory.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return workerFactory.isTerminated();
  }

  @Override
  public void shutdown() {
    workerFactory.shutdown();
  }

  @Override
  public void shutdownNow() {
    workerFactory.shutdownNow();
  }

  @Override
  public void awaitTermination(long timeout, TimeUnit unit) {
    workerFactory.awaitTermination(timeout, unit);
  }

  @Override
  public WorkerFactory getWorkerFactory() {
    return workerFactory;
  }

  public static class WorkflowServiceWrapper implements IWorkflowService {

    private final TestWorkflowService impl;

    public WorkflowServiceWrapper() {
      impl = new TestWorkflowService();
    }

    public long currentTimeMillis() {
      return impl.currentTimeMillis();
    }

    @Override
    public ClientOptions getOptions() {
      return impl.getOptions();
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
        RecordActivityTaskHeartbeatRequest heartbeatRequest) throws BaseError {
      return impl.RecordActivityTaskHeartbeat(heartbeatRequest);
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
        RecordActivityTaskHeartbeatByIDRequest heartbeatRequest) throws BaseError {
      return impl.RecordActivityTaskHeartbeatByID(heartbeatRequest);
    }

    @Override
    public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
        throws BaseError {
      impl.RespondActivityTaskCompleted(completeRequest);
    }

    @Override
    public void RespondActivityTaskCompletedByID(
        RespondActivityTaskCompletedByIDRequest completeRequest) throws BaseError {
      impl.RespondActivityTaskCompletedByID(completeRequest);
    }

    @Override
    public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
        throws BaseError {
      impl.RespondActivityTaskFailed(failRequest);
    }

    @Override
    public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
        throws BaseError {
      impl.RespondActivityTaskFailedByID(failRequest);
    }

    @Override
    public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
        throws BaseError {
      impl.RespondActivityTaskCanceled(canceledRequest);
    }

    @Override
    public void RespondActivityTaskCanceledByID(
        RespondActivityTaskCanceledByIDRequest canceledRequest) throws BaseError {
      impl.RespondActivityTaskCanceledByID(canceledRequest);
    }

    @Override
    public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
        throws BaseError {
      impl.RequestCancelWorkflowExecution(cancelRequest);
    }

    @Override
    public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
        throws BaseError {
      impl.SignalWorkflowExecution(signalRequest);
    }

    @Override
    public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
        SignalWithStartWorkflowExecutionRequest signalWithStartRequest) throws BaseError {
      return impl.SignalWithStartWorkflowExecution(signalWithStartRequest);
    }

    @Override
    public SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
        SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest)
        throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, EntityNotExistsError,
            ClientVersionNotSupportedError, BaseError {
      return impl.SignalWithStartWorkflowExecutionAsync(signalWithStartRequest);
    }

    @Override
    public ResetWorkflowExecutionResponse ResetWorkflowExecution(
        ResetWorkflowExecutionRequest resetRequest) throws BaseError {
      return impl.ResetWorkflowExecution(resetRequest);
    }

    @Override
    public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
        throws BaseError {
      impl.TerminateWorkflowExecution(terminateRequest);
    }

    @Override
    public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
        ListOpenWorkflowExecutionsRequest listRequest) throws BaseError {
      return impl.ListOpenWorkflowExecutions(listRequest);
    }

    @Override
    public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
        ListClosedWorkflowExecutionsRequest listRequest) throws BaseError {
      return impl.ListClosedWorkflowExecutions(listRequest);
    }

    @Override
    public ListWorkflowExecutionsResponse ListWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest) throws BaseError {
      return impl.ListWorkflowExecutions(listRequest);
    }

    @Override
    public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
        ListArchivedWorkflowExecutionsRequest listRequest) throws BaseError {
      return impl.ListArchivedWorkflowExecutions(listRequest);
    }

    @Override
    public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest) throws BaseError {
      return impl.ScanWorkflowExecutions(listRequest);
    }

    @Override
    public CountWorkflowExecutionsResponse CountWorkflowExecutions(
        CountWorkflowExecutionsRequest countRequest) throws BaseError {
      return impl.CountWorkflowExecutions(countRequest);
    }

    @Override
    public GetSearchAttributesResponse GetSearchAttributes() throws BaseError {
      return impl.GetSearchAttributes();
    }

    @Override
    public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
        throws BaseError {
      impl.RespondQueryTaskCompleted(completeRequest);
    }

    @Override
    public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
        throws BaseError {
      return impl.ResetStickyTaskList(resetRequest);
    }

    @Override
    public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, QueryFailedError,
            BaseError {
      return impl.QueryWorkflow(queryRequest);
    }

    @Override
    public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
        DescribeWorkflowExecutionRequest describeRequest) throws BaseError {
      return impl.DescribeWorkflowExecution(describeRequest);
    }

    @Override
    public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
        throws BaseError {
      return impl.DescribeTaskList(request);
    }

    @Override
    public ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, BaseError {
      return impl.GetClusterInfo();
    }

    @Override
    public ListTaskListPartitionsResponse ListTaskListPartitions(
        ListTaskListPartitionsRequest request) throws BaseError {
      return impl.ListTaskListPartitions(request);
    }

    @Override
    public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
        throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
            BaseError {
      impl.RefreshWorkflowTasks(request);
    }

    @Override
    public void RegisterDomain(
        RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler) throws BaseError {
      impl.RegisterDomain(registerRequest, resultHandler);
    }

    @Override
    public void DescribeDomain(
        DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler) throws BaseError {
      impl.DescribeDomain(describeRequest, resultHandler);
    }

    @Override
    public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ListDomains(listRequest, resultHandler);
    }

    @Override
    public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.UpdateDomain(updateRequest, resultHandler);
    }

    @Override
    public void DeprecateDomain(
        DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.DeprecateDomain(deprecateRequest, resultHandler);
    }

    @Override
    public void RestartWorkflowExecution(
        RestartWorkflowExecutionRequest restartRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RestartWorkflowExecution(restartRequest, resultHandler);
    }

    @Override
    public void GetTaskListsByDomain(
        GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler) throws BaseError {
      impl.GetTaskListsByDomain(request, resultHandler);
    }

    @Override
    public void StartWorkflowExecution(
        StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.StartWorkflowExecution(startRequest, resultHandler);
    }

    @Override
    public void StartWorkflowExecutionAsync(
        StartWorkflowExecutionAsyncRequest startRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.StartWorkflowExecutionAsync(startRequest, resultHandler);
    }

    @Override
    public void StartWorkflowExecutionWithTimeout(
        StartWorkflowExecutionRequest startRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws BaseError {
      impl.StartWorkflowExecutionWithTimeout(startRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void StartWorkflowExecutionAsyncWithTimeout(
        StartWorkflowExecutionAsyncRequest startAsyncRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws BaseError {
      impl.StartWorkflowExecutionAsyncWithTimeout(
          startAsyncRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void GetWorkflowExecutionHistory(
        GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.GetWorkflowExecutionHistory(getRequest, resultHandler);
    }

    @Override
    public void GetWorkflowExecutionHistoryWithTimeout(
        GetWorkflowExecutionHistoryRequest getRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws BaseError {
      impl.GetWorkflowExecutionHistoryWithTimeout(getRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
      return impl.isHealthy();
    }

    @Override
    public void PollForDecisionTask(
        PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.PollForDecisionTask(pollRequest, resultHandler);
    }

    @Override
    public void RespondDecisionTaskCompleted(
        RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondDecisionTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void RespondDecisionTaskFailed(
        RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondDecisionTaskFailed(failedRequest, resultHandler);
    }

    @Override
    public void PollForActivityTask(
        PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.PollForActivityTask(pollRequest, resultHandler);
    }

    @Override
    public void RecordActivityTaskHeartbeat(
        RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RecordActivityTaskHeartbeat(heartbeatRequest, resultHandler);
    }

    @Override
    public void RecordActivityTaskHeartbeatByID(
        RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RecordActivityTaskHeartbeatByID(heartbeatRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCompleted(
        RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCompletedByID(
        RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskCompletedByID(completeRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskFailed(
        RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskFailed(failRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskFailedByID(
        RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskFailedByID(failRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCanceled(
        RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskCanceled(canceledRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCanceledByID(
        RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondActivityTaskCanceledByID(canceledRequest, resultHandler);
    }

    @Override
    public void RequestCancelWorkflowExecution(
        RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RequestCancelWorkflowExecution(cancelRequest, resultHandler);
    }

    @Override
    public void SignalWorkflowExecution(
        SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.SignalWorkflowExecution(signalRequest, resultHandler);
    }

    @Override
    public void SignalWorkflowExecutionWithTimeout(
        SignalWorkflowExecutionRequest signalRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws BaseError {
      impl.SignalWorkflowExecutionWithTimeout(signalRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void SignalWithStartWorkflowExecution(
        SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
        AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.SignalWithStartWorkflowExecution(signalWithStartRequest, resultHandler);
    }

    @Override
    public void SignalWithStartWorkflowExecutionAsync(
        SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
        AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.SignalWithStartWorkflowExecutionAsync(signalWithStartRequest, resultHandler);
    }

    @Override
    public void ResetWorkflowExecution(
        ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ResetWorkflowExecution(resetRequest, resultHandler);
    }

    @Override
    public void TerminateWorkflowExecution(
        TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.TerminateWorkflowExecution(terminateRequest, resultHandler);
    }

    @Override
    public void ListOpenWorkflowExecutions(
        ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ListOpenWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListClosedWorkflowExecutions(
        ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ListClosedWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ListWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListArchivedWorkflowExecutions(
        ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ListArchivedWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ScanWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ScanWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void CountWorkflowExecutions(
        CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.CountWorkflowExecutions(countRequest, resultHandler);
    }

    @Override
    public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws BaseError {
      impl.GetSearchAttributes(resultHandler);
    }

    @Override
    public void RespondQueryTaskCompleted(
        RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.RespondQueryTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void ResetStickyTaskList(
        ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.ResetStickyTaskList(resetRequest, resultHandler);
    }

    @Override
    public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.QueryWorkflow(queryRequest, resultHandler);
    }

    @Override
    public void DescribeWorkflowExecution(
        DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.DescribeWorkflowExecution(describeRequest, resultHandler);
    }

    @Override
    public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler)
        throws BaseError {
      impl.DescribeTaskList(request, resultHandler);
    }

    @Override
    public void GetClusterInfo(AsyncMethodCallback resultHandler) throws BaseError {
      impl.GetClusterInfo(resultHandler);
    }

    @Override
    public void ListTaskListPartitions(
        ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler) throws BaseError {
      impl.ListTaskListPartitions(request, resultHandler);
    }

    @Override
    public void RefreshWorkflowTasks(
        RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler) throws BaseError {
      impl.RefreshWorkflowTasks(request, resultHandler);
    }

    @Override
    public void RegisterDomain(RegisterDomainRequest registerRequest)
        throws BadRequestError, InternalServiceError, DomainAlreadyExistsError, BaseError {
      impl.RegisterDomain(registerRequest);
    }

    @Override
    public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, BaseError {
      return impl.DescribeDomain(describeRequest);
    }

    @Override
    public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            BaseError {
      return impl.ListDomains(listRequest);
    }

    @Override
    public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, BaseError {
      return impl.UpdateDomain(updateRequest);
    }

    @Override
    public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, BaseError {
      impl.DeprecateDomain(deprecateRequest);
    }

    @Override
    public RestartWorkflowExecutionResponse RestartWorkflowExecution(
        RestartWorkflowExecutionRequest restartRequest)
        throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
            EntityNotExistsError, ClientVersionNotSupportedError, BaseError {
      return impl.RestartWorkflowExecution(restartRequest);
    }

    @Override
    public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
        throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
            ClientVersionNotSupportedError, BaseError {
      return impl.GetTaskListsByDomain(request);
    }

    @Override
    public StartWorkflowExecutionResponse StartWorkflowExecution(
        StartWorkflowExecutionRequest startRequest)
        throws BadRequestError, InternalServiceError, WorkflowExecutionAlreadyStartedError,
            ServiceBusyError, BaseError {
      return impl.StartWorkflowExecution(startRequest);
    }

    @Override
    public StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
        StartWorkflowExecutionAsyncRequest startRequest)
        throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, EntityNotExistsError,
            ClientVersionNotSupportedError, BaseError {
      return impl.StartWorkflowExecutionAsync(startRequest);
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
        GetWorkflowExecutionHistoryRequest getRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            BaseError {
      return impl.GetWorkflowExecutionHistory(getRequest);
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
        GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            BaseError {
      return impl.GetWorkflowExecutionHistoryWithTimeout(getRequest, timeoutInMillis);
    }

    @Override
    public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
        throws BadRequestError, InternalServiceError, ServiceBusyError, BaseError {
      return impl.PollForDecisionTask(pollRequest);
    }

    @Override
    public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
        RespondDecisionTaskCompletedRequest completeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, BaseError {
      return impl.RespondDecisionTaskCompleted(completeRequest);
    }

    @Override
    public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, BaseError {
      impl.RespondDecisionTaskFailed(failedRequest);
    }

    @Override
    public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
        throws BadRequestError, InternalServiceError, ServiceBusyError, BaseError {
      return impl.PollForActivityTask(pollRequest);
    }

    public void getDiagnostics(StringBuilder result) {
      impl.getDiagnostics(result);
    }

    @Override
    public void close() {
      impl.close();
    }

    public void registerDelayedCallback(Duration delay, Runnable r) {
      impl.registerDelayedCallback(delay, r);
    }

    public void lockTimeSkipping(String caller) {
      impl.lockTimeSkipping(caller);
    }

    public void unlockTimeSkipping(String caller) {
      impl.unlockTimeSkipping(caller);
    }

    public void sleep(Duration duration) {
      impl.sleep(duration);
    }
  }

  private static class TimeLockingInterceptor implements WorkflowClientInterceptor {

    private final WorkflowServiceWrapper service;

    TimeLockingInterceptor(WorkflowServiceWrapper service) {
      this.service = service;
    }

    @Override
    public WorkflowStub newUntypedWorkflowStub(
        String workflowType, WorkflowOptions options, WorkflowStub next) {
      return new TimeLockingWorkflowStub(service, next);
    }

    @Override
    public WorkflowStub newUntypedWorkflowStub(
        WorkflowExecution execution, Optional<String> workflowType, WorkflowStub next) {
      return new TimeLockingWorkflowStub(service, next);
    }

    @Override
    public ActivityCompletionClient newActivityCompletionClient(ActivityCompletionClient next) {
      return next;
    }

    private class TimeLockingWorkflowStub implements WorkflowStub {

      private final WorkflowServiceWrapper service;
      private final WorkflowStub next;

      TimeLockingWorkflowStub(WorkflowServiceWrapper service, WorkflowStub next) {
        this.service = service;
        this.next = next;
      }

      @Override
      public void signal(String signalName, Object... args) {
        next.signal(signalName, args);
      }

      @Override
      public CompletableFuture<Void> signalAsync(String signalName, Object... args) {
        return next.signalAsync(signalName, args);
      }

      @Override
      public CompletableFuture<Void> signalAsyncWithTimeout(
          long timeout, TimeUnit unit, String signalName, Object... args) {
        return next.signalAsyncWithTimeout(timeout, unit, signalName, args);
      }

      @Override
      public WorkflowExecution start(Object... args) {
        return next.start(args);
      }

      @Override
      public CompletableFuture<WorkflowExecution> startAsync(Object... args) {
        return next.startAsync(args);
      }

      @Override
      public CompletableFuture<WorkflowExecution> startAsyncWithTimeout(
          long timeout, TimeUnit unit, Object... args) {
        return next.startAsyncWithTimeout(timeout, unit, args);
      }

      @Override
      public WorkflowExecution enqueueStart(Object... args) {
        return next.enqueueStart(args);
      }

      @Override
      public CompletableFuture<WorkflowExecution> enqueueStartAsync(Object... args) {
        return next.enqueueStartAsync(args);
      }

      @Override
      public CompletableFuture<WorkflowExecution> enqueueStartAsyncWithTimeout(
          long timeout, TimeUnit unit, Object... args) {
        return next.enqueueStartAsyncWithTimeout(timeout, unit, args);
      }

      @Override
      public WorkflowExecution signalWithStart(
          String signalName, Object[] signalArgs, Object[] startArgs) {
        return next.signalWithStart(signalName, signalArgs, startArgs);
      }

      @Override
      public WorkflowExecution enqueueSignalWithStart(
          String signalName, Object[] signalArgs, Object[] startArgs) {
        return next.enqueueSignalWithStart(signalName, signalArgs, startArgs);
      }

      @Override
      public Optional<String> getWorkflowType() {
        return next.getWorkflowType();
      }

      @Override
      public WorkflowExecution getExecution() {
        return next.getExecution();
      }

      @Override
      public <R> R getResult(Class<R> resultClass, Type resultType) {
        service.unlockTimeSkipping("TimeLockingWorkflowStub getResult");
        try {
          return next.getResult(resultClass, resultType);
        } finally {
          service.lockTimeSkipping("TimeLockingWorkflowStub getResult");
        }
      }

      @Override
      public <R> R getResult(Class<R> resultClass) {
        service.unlockTimeSkipping("TimeLockingWorkflowStub getResult");
        try {
          return next.getResult(resultClass);
        } finally {
          service.lockTimeSkipping("TimeLockingWorkflowStub getResult");
        }
      }

      @Override
      public <R> CompletableFuture<R> getResultAsync(Class<R> resultClass, Type resultType) {
        return new TimeLockingFuture<>(next.getResultAsync(resultClass, resultType));
      }

      @Override
      public <R> CompletableFuture<R> getResultAsync(Class<R> resultClass) {
        return new TimeLockingFuture<>(next.getResultAsync(resultClass));
      }

      @Override
      public <R> R getResult(long timeout, TimeUnit unit, Class<R> resultClass, Type resultType)
          throws TimeoutException {
        service.unlockTimeSkipping("TimeLockingWorkflowStub getResult");
        try {
          return next.getResult(timeout, unit, resultClass, resultType);
        } finally {
          service.lockTimeSkipping("TimeLockingWorkflowStub getResult");
        }
      }

      @Override
      public <R> R getResult(long timeout, TimeUnit unit, Class<R> resultClass)
          throws TimeoutException {
        service.unlockTimeSkipping("TimeLockingWorkflowStub getResult");
        try {
          return next.getResult(timeout, unit, resultClass);
        } finally {
          service.lockTimeSkipping("TimeLockingWorkflowStub getResult");
        }
      }

      @Override
      public <R> CompletableFuture<R> getResultAsync(
          long timeout, TimeUnit unit, Class<R> resultClass, Type resultType) {
        return new TimeLockingFuture<>(next.getResultAsync(timeout, unit, resultClass, resultType));
      }

      @Override
      public <R> CompletableFuture<R> getResultAsync(
          long timeout, TimeUnit unit, Class<R> resultClass) {
        return new TimeLockingFuture<>(next.getResultAsync(timeout, unit, resultClass));
      }

      @Override
      public <R> R query(String queryType, Class<R> resultClass, Object... args) {
        return next.query(queryType, resultClass, args);
      }

      @Override
      public <R> R query(String queryType, Class<R> resultClass, Type resultType, Object... args) {
        return next.query(queryType, resultClass, resultType, args);
      }

      @Override
      public <R> R query(
          String queryType,
          Class<R> resultClass,
          QueryRejectCondition queryRejectCondition,
          Object... args) {
        return next.query(queryType, resultClass, queryRejectCondition, args);
      }

      @Override
      public <R> R query(
          String queryType,
          Class<R> resultClass,
          Type resultType,
          QueryRejectCondition queryRejectCondition,
          Object... args) {
        return next.query(queryType, resultClass, resultType, queryRejectCondition, args);
      }

      @Override
      public <R> R queryWithOptions(
          String queryType,
          QueryOptions options,
          Type resultType,
          Class<R> resultClass,
          Object... args) {
        return next.queryWithOptions(queryType, options, resultType, resultClass, args);
      }

      @Override
      public void cancel() {
        next.cancel();
      }

      @Override
      public Optional<WorkflowOptions> getOptions() {
        return next.getOptions();
      }

      /** Unlocks time skipping before blocking calls and locks back after completion. */
      private class TimeLockingFuture<R> extends CompletableFuture<R> {

        public TimeLockingFuture(CompletableFuture<R> resultAsync) {
          @SuppressWarnings({"FutureReturnValueIgnored", "unused"})
          CompletableFuture<R> ignored =
              resultAsync.whenComplete(
                  (r, e) -> {
                    service.lockTimeSkipping(
                        "TimeLockingWorkflowStub TimeLockingFuture constructor");
                    if (e == null) {
                      this.complete(r);
                    } else {
                      this.completeExceptionally(e);
                    }
                  });
        }

        @Override
        public R get() throws InterruptedException, ExecutionException {
          service.unlockTimeSkipping("TimeLockingWorkflowStub TimeLockingFuture get");
          try {
            return super.get();
          } finally {
            service.lockTimeSkipping("TimeLockingWorkflowStub TimeLockingFuture get");
          }
        }

        @Override
        public R get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
          service.unlockTimeSkipping("TimeLockingWorkflowStub TimeLockingFuture get");
          try {
            return super.get(timeout, unit);
          } finally {
            service.lockTimeSkipping("TimeLockingWorkflowStub TimeLockingFuture get");
          }
        }

        @Override
        public R join() {
          service.unlockTimeSkipping("TimeLockingWorkflowStub TimeLockingFuture join");
          return super.join();
        }
      }
    }
  }
}
