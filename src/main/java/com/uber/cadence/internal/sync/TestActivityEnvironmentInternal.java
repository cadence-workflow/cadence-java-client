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

import com.google.common.base.Defaults;
import com.uber.cadence.*;
import com.uber.cadence.GetTaskListsByDomainRequest;
import com.uber.cadence.GetTaskListsByDomainResponse;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.internal.metrics.NoopScope;
import com.uber.cadence.internal.worker.ActivityTaskHandler;
import com.uber.cadence.internal.worker.ActivityTaskHandler.Result;
import com.uber.cadence.serviceclient.AsyncMethodCallback;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.testing.TestEnvironmentOptions;
import com.uber.cadence.workflow.*;
import com.uber.cadence.workflow.Functions.Func;
import com.uber.cadence.workflow.Functions.Func1;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TestActivityEnvironmentInternal implements TestActivityEnvironment {

  private final POJOActivityTaskHandler activityTaskHandler;
  private final TestEnvironmentOptions testEnvironmentOptions;
  private final AtomicInteger idSequencer = new AtomicInteger();
  private ClassConsumerPair<Object> activityHeartbetListener;
  private static final ScheduledExecutorService heartbeatExecutor =
      Executors.newScheduledThreadPool(20);
  private IWorkflowService workflowService;

  public TestActivityEnvironmentInternal(TestEnvironmentOptions options) {
    if (options == null) {
      this.testEnvironmentOptions = new TestEnvironmentOptions.Builder().build();
    } else {
      this.testEnvironmentOptions = options;
    }
    activityTaskHandler =
        new POJOActivityTaskHandler(
            new WorkflowServiceWrapper(workflowService),
            testEnvironmentOptions.getWorkflowClientOptions().getDomain(),
            testEnvironmentOptions.getDataConverter(),
            heartbeatExecutor);
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
  @Override
  public void registerActivitiesImplementations(Object... activityImplementations) {
    activityTaskHandler.setActivitiesImplementation(activityImplementations);
  }

  /**
   * Creates client stub to activities that implement given interface.
   *
   * @param activityInterface interface type implemented by activities
   */
  @Override
  public <T> T newActivityStub(Class<T> activityInterface) {
    ActivityOptions options =
        new ActivityOptions.Builder().setScheduleToCloseTimeout(Duration.ofDays(1)).build();
    InvocationHandler invocationHandler =
        ActivityInvocationHandler.newInstance(
            options, new TestActivityExecutor(workflowService, null));
    invocationHandler = new DeterministicRunnerWrapper(invocationHandler);
    return ActivityInvocationHandlerBase.newProxy(activityInterface, invocationHandler);
  }

  @Override
  public <T> void setActivityHeartbeatListener(Class<T> detailsClass, Consumer<T> listener) {
    setActivityHeartbeatListener(detailsClass, detailsClass, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> void setActivityHeartbeatListener(
      Class<T> detailsClass, Type detailsType, Consumer<T> listener) {
    activityHeartbetListener = new ClassConsumerPair(detailsClass, detailsType, listener);
  }

  @Override
  public void setWorkflowService(IWorkflowService workflowService) {
    IWorkflowService service = new WorkflowServiceWrapper(workflowService);
    this.workflowService = service;
    this.activityTaskHandler.setWorkflowService(service);
  }

  private class TestActivityExecutor extends WorkflowInterceptorBase {

    @SuppressWarnings("UnusedVariable")
    private final IWorkflowService workflowService;

    TestActivityExecutor(IWorkflowService workflowService, WorkflowInterceptorBase next) {
      super(next);
      this.workflowService = workflowService;
    }

    @Override
    public <T> Promise<T> executeActivity(
        String activityType,
        Class<T> resultClass,
        Type resultType,
        Object[] args,
        ActivityOptions options) {
      PollForActivityTaskResponse task = new PollForActivityTaskResponse();
      task.setScheduleToCloseTimeoutSeconds((int) options.getScheduleToCloseTimeout().getSeconds());
      task.setHeartbeatTimeoutSeconds((int) options.getHeartbeatTimeout().getSeconds());
      task.setStartToCloseTimeoutSeconds((int) options.getStartToCloseTimeout().getSeconds());
      task.setScheduledTimestamp(Duration.ofMillis(System.currentTimeMillis()).toNanos());
      task.setStartedTimestamp(Duration.ofMillis(System.currentTimeMillis()).toNanos());
      task.setInput(testEnvironmentOptions.getDataConverter().toData(args));
      task.setTaskToken("test-task-token".getBytes(StandardCharsets.UTF_8));
      task.setActivityId(String.valueOf(idSequencer.incrementAndGet()));
      task.setWorkflowExecution(
          new WorkflowExecution()
              .setWorkflowId("test-workflow-id")
              .setRunId(UUID.randomUUID().toString()));
      task.setWorkflowType(new WorkflowType().setName("test-workflow"));
      task.setActivityType(new ActivityType().setName(activityType));
      Result taskResult = activityTaskHandler.handle(task, NoopScope.getInstance(), false);
      return Workflow.newPromise(getReply(task, taskResult, resultClass, resultType));
    }

    @Override
    public <R> Promise<R> executeLocalActivity(
        String activityName,
        Class<R> resultClass,
        Type resultType,
        Object[] args,
        LocalActivityOptions options) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <R> WorkflowResult<R> executeChildWorkflow(
        String workflowType,
        Class<R> resultClass,
        Type resultType,
        Object[] args,
        ChildWorkflowOptions options) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Random newRandom() {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Promise<Void> signalExternalWorkflow(
        String domain, WorkflowExecution execution, String signalName, Object[] args) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Promise<Void> signalExternalWorkflow(
        WorkflowExecution execution, String signalName, Object[] args) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Promise<Void> cancelWorkflow(WorkflowExecution execution) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void sleep(Duration duration) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean await(Duration timeout, String reason, Supplier<Boolean> unblockCondition) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void await(String reason, Supplier<Boolean> unblockCondition) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Promise<Void> newTimer(Duration duration) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <R> R sideEffect(Class<R> resultClass, Type resultType, Func<R> func) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <R> R mutableSideEffect(
        String id, Class<R> resultClass, Type resultType, BiPredicate<R, R> updated, Func<R> func) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getVersion(String changeID, int minSupported, int maxSupported) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void continueAsNew(
        Optional<String> workflowType, Optional<ContinueAsNewOptions> options, Object[] args) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void registerQuery(String queryType, Type[] argTypes, Func1<Object[], Object> callback) {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public UUID randomUUID() {
      throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void upsertSearchAttributes(Map<String, Object> searchAttributes) {
      throw new UnsupportedOperationException("not implemented");
    }

    private <T> T getReply(
        PollForActivityTaskResponse task,
        ActivityTaskHandler.Result response,
        Class<T> resultClass,
        Type resultType) {
      RespondActivityTaskCompletedRequest taskCompleted = response.getTaskCompleted();
      if (taskCompleted != null) {
        return testEnvironmentOptions
            .getDataConverter()
            .fromData(taskCompleted.getResult(), resultClass, resultType);
      } else {
        RespondActivityTaskFailedRequest taskFailed =
            response.getTaskFailedResult().getTaskFailedRequest();
        if (taskFailed != null) {
          String causeClassName = taskFailed.getReason();
          Class<? extends Exception> causeClass;
          Exception cause;
          try {
            @SuppressWarnings("unchecked") // cc is just to have a place to put this annotation
            Class<? extends Exception> cc =
                (Class<? extends Exception>) Class.forName(causeClassName);
            causeClass = cc;
            cause =
                testEnvironmentOptions
                    .getDataConverter()
                    .fromData(taskFailed.getDetails(), causeClass, causeClass);
          } catch (Exception e) {
            cause = e;
          }
          throw new ActivityFailureException(
              0, task.getActivityType(), task.getActivityId(), cause);

        } else {
          RespondActivityTaskCanceledRequest taskCancelled = response.getTaskCancelled();
          if (taskCancelled != null) {
            throw new CancellationException(
                new String(taskCancelled.getDetails(), StandardCharsets.UTF_8));
          }
        }
      }
      return Defaults.defaultValue(resultClass);
    }
  }

  private static class ClassConsumerPair<T> {

    final Consumer<T> consumer;
    final Class<T> valueClass;
    final Type valueType;

    ClassConsumerPair(Class<T> valueClass, Type valueType, Consumer<T> consumer) {
      this.valueClass = Objects.requireNonNull(valueClass);
      this.valueType = Objects.requireNonNull(valueType);
      this.consumer = Objects.requireNonNull(consumer);
    }
  }

  private class WorkflowServiceWrapper implements IWorkflowService {

    private final IWorkflowService impl;

    @Override
    public ClientOptions getOptions() {
      return impl.getOptions();
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
      return impl.isHealthy();
    }

    private WorkflowServiceWrapper(IWorkflowService impl) {
      if (impl == null) {
        // Create empty implementation that just ignores all requests.
        this.impl =
            (IWorkflowService)
                Proxy.newProxyInstance(
                    WorkflowServiceWrapper.class.getClassLoader(),
                    new Class<?>[] {IWorkflowService.class},
                    (proxy, method, args) -> {
                      // noop
                      return method.getReturnType().getDeclaredConstructor().newInstance();
                    });
      } else {
        this.impl = impl;
      }
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
        RecordActivityTaskHeartbeatRequest heartbeatRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      if (activityHeartbetListener != null) {
        Object details =
            testEnvironmentOptions
                .getDataConverter()
                .fromData(
                    heartbeatRequest.getDetails(),
                    activityHeartbetListener.valueClass,
                    activityHeartbetListener.valueType);
        activityHeartbetListener.consumer.accept(details);
      }
      // TODO: Cancellation
      return impl.RecordActivityTaskHeartbeat(heartbeatRequest);
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
        RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, DomainNotActiveError, LimitExceededError,
            ServiceBusyError, CadenceError {
      return impl.RecordActivityTaskHeartbeatByID(heartbeatRequest);
    }

    @Override
    public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskCompleted(completeRequest);
    }

    @Override
    public void RespondActivityTaskCompletedByID(
        RespondActivityTaskCompletedByIDRequest completeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskCompletedByID(completeRequest);
    }

    @Override
    public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskFailed(failRequest);
    }

    @Override
    public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskFailedByID(failRequest);
    }

    @Override
    public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskCanceled(canceledRequest);
    }

    @Override
    public void RespondActivityTaskCanceledByID(
        RespondActivityTaskCanceledByIDRequest canceledRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondActivityTaskCanceledByID(canceledRequest);
    }

    @Override
    public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            CancellationAlreadyRequestedError, ServiceBusyError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RequestCancelWorkflowExecution(cancelRequest);
    }

    @Override
    public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, ServiceBusyError, CadenceError {
      impl.SignalWorkflowExecution(signalRequest);
    }

    @Override
    public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
        SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, WorkflowExecutionAlreadyStartedError,
            CadenceError {
      return impl.SignalWithStartWorkflowExecution(signalWithStartRequest);
    }

    @Override
    public SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
        SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest)
        throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, EntityNotExistsError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.SignalWithStartWorkflowExecutionAsync(signalWithStartRequest);
    }

    @Override
    public ResetWorkflowExecutionResponse ResetWorkflowExecution(
        ResetWorkflowExecutionRequest resetRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, ClientVersionNotSupportedError, CadenceError {
      return impl.ResetWorkflowExecution(resetRequest);
    }

    @Override
    public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, ServiceBusyError, CadenceError {
      impl.TerminateWorkflowExecution(terminateRequest);
    }

    @Override
    public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
        ListOpenWorkflowExecutionsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            CadenceError {
      return impl.ListOpenWorkflowExecutions(listRequest);
    }

    @Override
    public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
        ListClosedWorkflowExecutionsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            CadenceError {
      return impl.ListClosedWorkflowExecutions(listRequest);
    }

    @Override
    public ListWorkflowExecutionsResponse ListWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.ListWorkflowExecutions(listRequest);
    }

    @Override
    public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
        ListArchivedWorkflowExecutionsRequest listRequest)
        throws BadRequestError, EntityNotExistsError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.ListArchivedWorkflowExecutions(listRequest);
    }

    @Override
    public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.ScanWorkflowExecutions(listRequest);
    }

    @Override
    public CountWorkflowExecutionsResponse CountWorkflowExecutions(
        CountWorkflowExecutionsRequest countRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.CountWorkflowExecutions(countRequest);
    }

    @Override
    public GetSearchAttributesResponse GetSearchAttributes()
        throws InternalServiceError, ServiceBusyError, ClientVersionNotSupportedError,
            CadenceError {
      return impl.GetSearchAttributes();
    }

    @Override
    public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondQueryTaskCompleted(completeRequest);
    }

    @Override
    public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, LimitExceededError,
            ServiceBusyError, DomainNotActiveError, CadenceError {
      return impl.ResetStickyTaskList(resetRequest);
    }

    @Override
    public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, QueryFailedError,
            CadenceError {
      return impl.QueryWorkflow(queryRequest);
    }

    @Override
    public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
        DescribeWorkflowExecutionRequest describeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, CadenceError {
      return impl.DescribeWorkflowExecution(describeRequest);
    }

    @Override
    public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, CadenceError {
      return impl.DescribeTaskList(request);
    }

    @Override
    public ClusterInfo GetClusterInfo()
        throws InternalServiceError, ServiceBusyError, CadenceError {
      return impl.GetClusterInfo();
    }

    @Override
    public ListTaskListPartitionsResponse ListTaskListPartitions(
        ListTaskListPartitionsRequest request)
        throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
            CadenceError {
      return impl.ListTaskListPartitions(request);
    }

    @Override
    public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
        throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
            CadenceError {
      impl.RefreshWorkflowTasks(request);
    }

    @Override
    public void RegisterDomain(
        RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RegisterDomain(registerRequest, resultHandler);
    }

    @Override
    public void DescribeDomain(
        DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.DescribeDomain(describeRequest, resultHandler);
    }

    @Override
    public void DiagnoseWorkflowExecution(
        DiagnoseWorkflowExecutionRequest diagnoseRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.DiagnoseWorkflowExecution(diagnoseRequest, resultHandler);
    }

    @Override
    public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListDomains(listRequest, resultHandler);
    }

    @Override
    public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.UpdateDomain(updateRequest, resultHandler);
    }

    @Override
    public void DeprecateDomain(
        DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.DeprecateDomain(deprecateRequest, resultHandler);
    }

    @Override
    public void RestartWorkflowExecution(
        RestartWorkflowExecutionRequest restartRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RestartWorkflowExecution(restartRequest, resultHandler);
    }

    @Override
    public void GetTaskListsByDomain(
        GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.GetTaskListsByDomain(request, resultHandler);
    }

    @Override
    public void StartWorkflowExecution(
        StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.StartWorkflowExecution(startRequest, resultHandler);
    }

    @Override
    public void StartWorkflowExecutionAsync(
        StartWorkflowExecutionAsyncRequest startRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.StartWorkflowExecutionAsync(startRequest, resultHandler);
    }

    @Override
    public void StartWorkflowExecutionWithTimeout(
        StartWorkflowExecutionRequest startRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws CadenceError {
      impl.StartWorkflowExecutionWithTimeout(startRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void StartWorkflowExecutionAsyncWithTimeout(
        StartWorkflowExecutionAsyncRequest startAsyncRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws CadenceError {
      impl.StartWorkflowExecutionAsyncWithTimeout(
          startAsyncRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void GetWorkflowExecutionHistory(
        GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.GetWorkflowExecutionHistory(getRequest, resultHandler);
    }

    @Override
    public void GetWorkflowExecutionHistoryWithTimeout(
        GetWorkflowExecutionHistoryRequest getRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws CadenceError {
      impl.GetWorkflowExecutionHistoryWithTimeout(getRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void PollForDecisionTask(
        PollForDecisionTaskRequest pollRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.PollForDecisionTask(pollRequest, resultHandler);
    }

    @Override
    public void RespondDecisionTaskCompleted(
        RespondDecisionTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondDecisionTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void RespondDecisionTaskFailed(
        RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondDecisionTaskFailed(failedRequest, resultHandler);
    }

    @Override
    public void PollForActivityTask(
        PollForActivityTaskRequest pollRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.PollForActivityTask(pollRequest, resultHandler);
    }

    @Override
    public void RecordActivityTaskHeartbeat(
        RecordActivityTaskHeartbeatRequest heartbeatRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RecordActivityTaskHeartbeat(heartbeatRequest, resultHandler);
    }

    @Override
    public void RecordActivityTaskHeartbeatByID(
        RecordActivityTaskHeartbeatByIDRequest heartbeatRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RecordActivityTaskHeartbeatByID(heartbeatRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCompleted(
        RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCompletedByID(
        RespondActivityTaskCompletedByIDRequest completeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskCompletedByID(completeRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskFailed(
        RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskFailed(failRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskFailedByID(
        RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskFailedByID(failRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCanceled(
        RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskCanceled(canceledRequest, resultHandler);
    }

    @Override
    public void RespondActivityTaskCanceledByID(
        RespondActivityTaskCanceledByIDRequest canceledRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondActivityTaskCanceledByID(canceledRequest, resultHandler);
    }

    @Override
    public void RequestCancelWorkflowExecution(
        RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RequestCancelWorkflowExecution(cancelRequest, resultHandler);
    }

    @Override
    public void SignalWorkflowExecution(
        SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.SignalWorkflowExecution(signalRequest, resultHandler);
    }

    @Override
    public void SignalWorkflowExecutionWithTimeout(
        SignalWorkflowExecutionRequest signalRequest,
        AsyncMethodCallback resultHandler,
        Long timeoutInMillis)
        throws CadenceError {
      impl.SignalWorkflowExecutionWithTimeout(signalRequest, resultHandler, timeoutInMillis);
    }

    @Override
    public void SignalWithStartWorkflowExecution(
        SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
        AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.SignalWithStartWorkflowExecution(signalWithStartRequest, resultHandler);
    }

    @Override
    public void SignalWithStartWorkflowExecutionAsync(
        SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
        AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.SignalWithStartWorkflowExecutionAsync(signalWithStartRequest, resultHandler);
    }

    @Override
    public void ResetWorkflowExecution(
        ResetWorkflowExecutionRequest resetRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ResetWorkflowExecution(resetRequest, resultHandler);
    }

    @Override
    public void TerminateWorkflowExecution(
        TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.TerminateWorkflowExecution(terminateRequest, resultHandler);
    }

    @Override
    public void ListOpenWorkflowExecutions(
        ListOpenWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListOpenWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListClosedWorkflowExecutions(
        ListClosedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListClosedWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ListArchivedWorkflowExecutions(
        ListArchivedWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListArchivedWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void ScanWorkflowExecutions(
        ListWorkflowExecutionsRequest listRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ScanWorkflowExecutions(listRequest, resultHandler);
    }

    @Override
    public void CountWorkflowExecutions(
        CountWorkflowExecutionsRequest countRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.CountWorkflowExecutions(countRequest, resultHandler);
    }

    @Override
    public void GetSearchAttributes(AsyncMethodCallback resultHandler) throws CadenceError {
      impl.GetSearchAttributes(resultHandler);
    }

    @Override
    public void RespondQueryTaskCompleted(
        RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RespondQueryTaskCompleted(completeRequest, resultHandler);
    }

    @Override
    public void ResetStickyTaskList(
        ResetStickyTaskListRequest resetRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ResetStickyTaskList(resetRequest, resultHandler);
    }

    @Override
    public void QueryWorkflow(QueryWorkflowRequest queryRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.QueryWorkflow(queryRequest, resultHandler);
    }

    @Override
    public void DescribeWorkflowExecution(
        DescribeWorkflowExecutionRequest describeRequest, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.DescribeWorkflowExecution(describeRequest, resultHandler);
    }

    @Override
    public void DescribeTaskList(DescribeTaskListRequest request, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.DescribeTaskList(request, resultHandler);
    }

    @Override
    public void GetClusterInfo(AsyncMethodCallback resultHandler) throws CadenceError {
      impl.GetClusterInfo(resultHandler);
    }

    @Override
    public void ListTaskListPartitions(
        ListTaskListPartitionsRequest request, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.ListTaskListPartitions(request, resultHandler);
    }

    @Override
    public void RefreshWorkflowTasks(
        RefreshWorkflowTasksRequest request, AsyncMethodCallback resultHandler)
        throws CadenceError {
      impl.RefreshWorkflowTasks(request, resultHandler);
    }

    @Override
    public void RegisterDomain(RegisterDomainRequest registerRequest)
        throws BadRequestError, InternalServiceError, DomainAlreadyExistsError, CadenceError {
      impl.RegisterDomain(registerRequest);
    }

    @Override
    public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, CadenceError {
      return impl.DescribeDomain(describeRequest);
    }

    @Override
    public DiagnoseWorkflowExecutionResponse DiagnoseWorkflowExecution(
        DiagnoseWorkflowExecutionRequest diagnoseRequest)
        throws DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.DiagnoseWorkflowExecution(diagnoseRequest);
    }

    @Override
    public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            CadenceError {
      return impl.ListDomains(listRequest);
    }

    @Override
    public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, CadenceError {
      return impl.UpdateDomain(updateRequest);
    }

    @Override
    public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
        throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      impl.DeprecateDomain(deprecateRequest);
    }

    @Override
    public RestartWorkflowExecutionResponse RestartWorkflowExecution(
        RestartWorkflowExecutionRequest restartRequest)
        throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
            EntityNotExistsError, ClientVersionNotSupportedError, CadenceError {
      return impl.RestartWorkflowExecution(restartRequest);
    }

    @Override
    public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
        throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.GetTaskListsByDomain(request);
    }

    @Override
    public StartWorkflowExecutionResponse StartWorkflowExecution(
        StartWorkflowExecutionRequest startRequest)
        throws BadRequestError, InternalServiceError, WorkflowExecutionAlreadyStartedError,
            ServiceBusyError, CadenceError {
      return impl.StartWorkflowExecution(startRequest);
    }

    @Override
    public StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
        StartWorkflowExecutionAsyncRequest startRequest)
        throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
            DomainNotActiveError, LimitExceededError, EntityNotExistsError,
            ClientVersionNotSupportedError, CadenceError {
      return impl.StartWorkflowExecutionAsync(startRequest);
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
        GetWorkflowExecutionHistoryRequest getRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            CadenceError {
      return impl.GetWorkflowExecutionHistory(getRequest);
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
        GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis)
        throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError,
            CadenceError {
      return impl.GetWorkflowExecutionHistoryWithTimeout(getRequest, timeoutInMillis);
    }

    @Override
    public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
        throws BadRequestError, InternalServiceError, ServiceBusyError, CadenceError {
      return impl.PollForDecisionTask(pollRequest);
    }

    @Override
    public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
        RespondDecisionTaskCompletedRequest completeRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      return impl.RespondDecisionTaskCompleted(completeRequest);
    }

    @Override
    public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
        throws BadRequestError, InternalServiceError, EntityNotExistsError,
            WorkflowExecutionAlreadyCompletedError, CadenceError {
      impl.RespondDecisionTaskFailed(failedRequest);
    }

    @Override
    public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
        throws BadRequestError, InternalServiceError, ServiceBusyError, CadenceError {
      return impl.PollForActivityTask(pollRequest);
    }

    @Override
    public void close() {
      impl.close();
    }
  }
}
