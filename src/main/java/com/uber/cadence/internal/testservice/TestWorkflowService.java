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

package com.uber.cadence.internal.testservice;

import com.uber.cadence.api.v1.*;
import com.uber.cadence.internal.testservice.TestWorkflowMutableStateImpl.QueryId;
import com.uber.cadence.internal.testservice.TestWorkflowStore.WorkflowState;
import com.uber.cadence.serviceclient.CallMetaData;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowServiceV4;
import com.uber.cadence.serviceclient.exceptions.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In memory implementation of the Cadence service. To be used for testing purposes only. Do not use
 * directly. Instead use {@link com.uber.cadence.testing.TestWorkflowEnvironment}.
 */
public final class TestWorkflowService implements IWorkflowServiceV4 {
  private static final Logger log = LoggerFactory.getLogger(TestWorkflowService.class);

  private final Lock lock = new ReentrantLock();

  private final TestWorkflowStore store = new TestWorkflowStoreImpl();

  private final Map<ExecutionId, TestWorkflowMutableState> executions = new HashMap<>();

  // key->WorkflowId
  private final Map<WorkflowId, TestWorkflowMutableState> executionsByWorkflowId = new HashMap<>();

  private final ForkJoinPool forkJoinPool = new ForkJoinPool(4);

  private TestWorkflowMutableState getMutableState(ExecutionId executionId)
      throws InternalServiceException, EntityNotExistsException {
    return getMutableState(executionId, true);
  }

  private TestWorkflowMutableState getMutableState(ExecutionId executionId, boolean failNotExists)
      throws InternalServiceException, EntityNotExistsException {
    lock.lock();
    try {
      if (executionId.getExecution().getRunId() == null) {
        return getMutableState(executionId.getWorkflowId(), failNotExists);
      }
      TestWorkflowMutableState mutableState = executions.get(executionId);
      if (mutableState == null && failNotExists) {
        throw new InternalServiceException("Execution not found in mutable state: " + executionId);
      }
      return mutableState;
    } finally {
      lock.unlock();
    }
  }

  private TestWorkflowMutableState getMutableState(WorkflowId workflowId)
      throws EntityNotExistsException {
    return getMutableState(workflowId, true);
  }

  private TestWorkflowMutableState getMutableState(WorkflowId workflowId, boolean failNotExists)
      throws EntityNotExistsException {
    lock.lock();
    try {
      TestWorkflowMutableState mutableState = executionsByWorkflowId.get(workflowId);
      if (mutableState == null && failNotExists) {
        throw new EntityNotExistsException("Execution not found in mutable state: " + workflowId);
      }
      return mutableState;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Blocking blockingStub() {
    return new Blocking() {
      @Override
      public StartWorkflowExecutionResponse startWorkflowExecution(
          StartWorkflowExecutionRequest request, @Nullable CallMetaData meta) {
        return startWorkflowExecutionImpl(
            request, 0, Optional.empty(), OptionalLong.empty(), Optional.empty());
      }

      @Override
      public StartWorkflowExecutionAsyncResponse startWorkflowExecutionAsync(
          StartWorkflowExecutionAsyncRequest request, @Nullable CallMetaData meta) {
        startWorkflowExecution(request.getRequest(), meta);
        // Just run it
        return StartWorkflowExecutionAsyncResponse.getDefaultInstance();
      }

      @Override
      public GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistory(
          GetWorkflowExecutionHistoryRequest getRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ServiceClientException {
        ExecutionId executionId =
            new ExecutionId(getRequest.getDomain(), getRequest.getWorkflowExecution());
        TestWorkflowMutableState mutableState = getMutableState(executionId);

        return store.getWorkflowExecutionHistory(mutableState.getExecutionId(), getRequest);
      }

      @Override
      public PollForDecisionTaskResponse pollForDecisionTask(
          PollForDecisionTaskRequest pollRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, ServiceBusyException,
              ServiceClientException {
        PollForDecisionTaskResponse task;
        try {
          task = store.pollForDecisionTask(pollRequest);
        } catch (InterruptedException e) {
          return PollForDecisionTaskResponse.getDefaultInstance();
        }
        ExecutionId executionId =
            new ExecutionId(pollRequest.getDomain(), task.getWorkflowExecution());
        TestWorkflowMutableState mutableState = getMutableState(executionId);
        try {
          mutableState.startDecisionTask(task, pollRequest);
          // The task always has the original tasklist is was created on as part of the response.
          // This
          // may different
          // then the task list it was scheduled on as in the case of sticky execution.
          return task.toBuilder()
              .setWorkflowExecutionTaskList(mutableState.getStartRequest().getTaskList())
              .build();
          ;
        } catch (EntityNotExistsException e) {
          if (log.isDebugEnabled()) {
            log.debug("Skipping outdated decision task for " + executionId, e);
          }
          // skip the task
        }
        return task.toBuilder()
            .setWorkflowExecutionTaskList(mutableState.getStartRequest().getTaskList())
            .build();
        ;
      }

      @Override
      public RespondDecisionTaskCompletedResponse respondDecisionTaskCompleted(
          RespondDecisionTaskCompletedRequest request, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        DecisionTaskToken taskToken =
            DecisionTaskToken.fromBytes(request.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(taskToken.getExecutionId());
        mutableState.completeDecisionTask(taskToken.getHistorySize(), request);
        return RespondDecisionTaskCompletedResponse.getDefaultInstance();
      }

      @Override
      public RespondDecisionTaskFailedResponse respondDecisionTaskFailed(
          RespondDecisionTaskFailedRequest failedRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        DecisionTaskToken taskToken =
            DecisionTaskToken.fromBytes(failedRequest.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(taskToken.getExecutionId());
        mutableState.failDecisionTask(failedRequest);
        return RespondDecisionTaskFailedResponse.getDefaultInstance();
      }

      @Override
      public PollForActivityTaskResponse pollForActivityTask(
          PollForActivityTaskRequest pollRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, ServiceBusyException,
              ServiceClientException {
        PollForActivityTaskResponse task;
        while (true) {
          try {
            task = store.pollForActivityTask(pollRequest);
          } catch (InterruptedException e) {
            return PollForActivityTaskResponse.getDefaultInstance();
          }
          ExecutionId executionId =
              new ExecutionId(pollRequest.getDomain(), task.getWorkflowExecution());
          TestWorkflowMutableState mutableState = getMutableState(executionId);
          try {
            mutableState.startActivityTask(task, pollRequest);
            return task;
          } catch (EntityNotExistsException e) {
            if (log.isDebugEnabled()) {
              log.debug("Skipping outdated activity task for " + executionId, e);
            }
          }
        }
      }

      @Override
      public RecordActivityTaskHeartbeatResponse recordActivityTaskHeartbeat(
          RecordActivityTaskHeartbeatRequest heartbeatRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        ActivityId activityId = ActivityId.fromBytes(heartbeatRequest.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(activityId.getExecutionId());
        return mutableState.heartbeatActivityTask(
            activityId.getId(), heartbeatRequest.getDetails().toByteArray());
      }

      @Override
      public RespondActivityTaskCompletedResponse respondActivityTaskCompleted(
          RespondActivityTaskCompletedRequest completeRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        ActivityId activityId = ActivityId.fromBytes(completeRequest.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(activityId.getExecutionId());
        mutableState.completeActivityTask(activityId.getId(), completeRequest);
      }

      @Override
      public RespondActivityTaskFailedResponse respondActivityTaskFailed(
          RespondActivityTaskFailedRequest failRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        ActivityId activityId = ActivityId.fromBytes(failRequest.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(activityId.getExecutionId());
        mutableState.failActivityTask(activityId.getId(), failRequest);
      }

      @Override
      public RespondActivityTaskCanceledResponse respondActivityTaskCanceled(
          RespondActivityTaskCanceledRequest canceledRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        ActivityId activityId = ActivityId.fromBytes(canceledRequest.getTaskToken().toByteArray());
        TestWorkflowMutableState mutableState = getMutableState(activityId.getExecutionId());
        mutableState.cancelActivityTask(activityId.getId(), canceledRequest);
        return RespondActivityTaskCanceledResponse.getDefaultInstance();
      }

      @Override
      public RespondActivityTaskCanceledByIDResponse respondActivityTaskCanceledByID(
          RespondActivityTaskCanceledByIDRequest canceledRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        ActivityId activityId =
            new ActivityId(
                canceledRequest.getDomain(),
                canceledRequest.getWorkflowExecution().getWorkflowId(),
                canceledRequest.getWorkflowExecution().getRunId(),
                canceledRequest.getActivityId());
        TestWorkflowMutableState mutableState = getMutableState(activityId.getWorkflowId());
        mutableState.cancelActivityTaskById(activityId.getId(), canceledRequest);
        return RespondActivityTaskCanceledByIDResponse.getDefaultInstance();
      }

      @Override
      public RequestCancelWorkflowExecutionResponse requestCancelWorkflowExecution(
          RequestCancelWorkflowExecutionRequest cancelRequest, @Nullable CallMetaData meta)
          throws ServiceClientException {
        ExecutionId executionId =
            new ExecutionId(cancelRequest.getDomain(), cancelRequest.getWorkflowExecution());
        TestWorkflowMutableState mutableState = getMutableState(executionId);
        mutableState.requestCancelWorkflowExecution(cancelRequest);
        return RequestCancelWorkflowExecutionResponse.getDefaultInstance();
      }

      @Override
      public SignalWorkflowExecutionResponse signalWorkflowExecution(
          SignalWorkflowExecutionRequest signalRequest, @Nullable CallMetaData meta)
          throws ServiceClientException {
        ExecutionId executionId =
            new ExecutionId(signalRequest.getDomain(), signalRequest.getWorkflowExecution());
        TestWorkflowMutableState mutableState = getMutableState(executionId);
        mutableState.signal(signalRequest);
        return SignalWorkflowExecutionResponse.getDefaultInstance();
      }

      @Override
      public SignalWithStartWorkflowExecutionResponse signalWithStartWorkflowExecution(
          SignalWithStartWorkflowExecutionRequest r, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, DomainNotActiveException, LimitExceededException,
              WorkflowExecutionAlreadyStartedException, ServiceClientException {
        ExecutionId executionId =
            new ExecutionId(
                r.getStartRequest().getDomain(), r.getStartRequest().getWorkflowId(), null);
        TestWorkflowMutableState mutableState = getMutableState(executionId, false);
        SignalWorkflowExecutionRequest signalRequest =
            SignalWorkflowExecutionRequest.newBuilder()
                .setSignalInput(r.getSignalInput())
                .setSignalName(r.getSignalName())
                .setControl(r.getControl())
                .setDomain(r.getStartRequest().getDomain())
                .setWorkflowExecution(executionId.getExecution())
                .setRequestId(r.getRequestId())
                .setIdentity(r.getIdentity())
                .build();
        if (mutableState != null) {
          mutableState.signal(signalRequest);
          return SignalWithStartWorkflowExecutionResponse.newBuilder()
              .setRunId(mutableState.getExecutionId().getExecution().getRunId())
              .build();
        }
        StartWorkflowExecutionRequest startRequest = r.getStartRequest();
        return startWorkflowExecutionImpl(
            startRequest, 0, Optional.empty(), OptionalLong.empty(), Optional.of(signalRequest));
      }

      @Override
      public SignalWithStartWorkflowExecutionAsyncResponse signalWithStartWorkflowExecutionAsync(
          SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
          @Nullable CallMetaData meta)
          throws BadRequestException, WorkflowExecutionAlreadyStartedException,
              ServiceBusyException, DomainNotActiveException, LimitExceededException,
              EntityNotExistsException, ClientVersionNotSupportedException, ServiceClientException {
        signalWithStartWorkflowExecution(signalWithStartRequest.getRequest(), meta);
        return SignalWithStartWorkflowExecutionAsyncResponse.getDefaultInstance();
      }

      @Override
      public ListOpenWorkflowExecutionsResponse listOpenWorkflowExecutions(
          ListOpenWorkflowExecutionsRequest listRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ServiceClientException {
        Optional<String> workflowIdFilter;
        WorkflowExecutionFilter executionFilter = listRequest.getExecutionFilter();
        if (executionFilter != null
            && executionFilter.isSetWorkflowId()
            && !executionFilter.getWorkflowId().isEmpty()) {
          workflowIdFilter = Optional.of(executionFilter.getWorkflowId());
        } else {
          workflowIdFilter = Optional.empty();
        }
        List<WorkflowExecutionInfo> result =
            store.listWorkflows(WorkflowState.OPEN, workflowIdFilter);
        return ListOpenWorkflowExecutionsResponse.newBuilder().addAllExecutions(result).build();
      }

      @Override
      public ListClosedWorkflowExecutionsResponse listClosedWorkflowExecutions(
          ListClosedWorkflowExecutionsRequest listRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ServiceClientException {
        Optional<String> workflowIdFilter;
        WorkflowExecutionFilter executionFilter = listRequest.getExecutionFilter();
        if (executionFilter != null
            && executionFilter.isSetWorkflowId()
            && !executionFilter.getWorkflowId().isEmpty()) {
          workflowIdFilter = Optional.of(executionFilter.getWorkflowId());
        } else {
          workflowIdFilter = Optional.empty();
        }
        List<WorkflowExecutionInfo> result =
            store.listWorkflows(WorkflowState.CLOSED, workflowIdFilter);
        return ListClosedWorkflowExecutionsResponse.newBuilder().setExecutions(result);
      }

      @Override
      public ListWorkflowExecutionsResponse listWorkflowExecutions(
          ListWorkflowExecutionsRequest listRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ClientVersionNotSupportedException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public ListArchivedWorkflowExecutionsResponse listArchivedWorkflowExecutions(
          ListArchivedWorkflowExecutionsRequest listRequest, @Nullable CallMetaData meta)
          throws BadRequestException, EntityNotExistsException, ServiceBusyException,
              ClientVersionNotSupportedException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public ScanWorkflowExecutionsResponse scanWorkflowExecutions(
          ScanWorkflowExecutionsRequest listRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ClientVersionNotSupportedException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public CountWorkflowExecutionsResponse countWorkflowExecutions(
          CountWorkflowExecutionsRequest countRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ClientVersionNotSupportedException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public RespondQueryTaskCompletedResponse respondQueryTaskCompleted(
          RespondQueryTaskCompletedRequest completeRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        QueryId queryId = QueryId.fromBytes(completeRequest.getTaskToken());
        TestWorkflowMutableState mutableState = getMutableState(queryId.getExecutionId());
        mutableState.completeQuery(queryId, completeRequest);
      }

      @Override
      public ResetStickyTaskListResponse resetStickyTaskList(
          ResetStickyTaskListRequest resetRequest, @Nullable CallMetaData meta)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              LimitExceededException, ServiceBusyException, DomainNotActiveException,
              ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              QueryFailedError, ServiceClientException {
        ExecutionId executionId =
            new ExecutionId(queryRequest.getDomain(), queryRequest.getExecution());
        TestWorkflowMutableState mutableState = getMutableState(executionId);
        return mutableState.query(queryRequest);
      }

      @Override
      public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
          DescribeWorkflowExecutionRequest describeRequest)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public ClusterInfo GetClusterInfo()
          throws InternalServiceException, ServiceBusyException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public ListTaskListPartitionsResponse ListTaskListPartitions(
          ListTaskListPartitionsRequest request)
          throws BadRequestException, EntityNotExistsException, LimitExceededException,
              ServiceBusyException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
          throws BadRequestException, DomainNotActiveException, ServiceBusyException,
              EntityNotExistsException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void RegisterDomain(
          RegisterDomainRequest registerRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void DescribeDomain(
          DescribeDomainRequest describeRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void ListDomains(ListDomainsRequest listRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void UpdateDomain(UpdateDomainRequest updateRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void DeprecateDomain(
          DeprecateDomainRequest deprecateRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void RestartWorkflowExecution(
          RestartWorkflowExecutionRequest restartRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void GetTaskListsByDomain(
          GetTaskListsByDomainRequest request, AsyncMethodCallback resultHandler)
          throws org.apache.thrift.TException {
        throw new UnsupportedOperationException("not implemented");
      }

      @Override
      public void StartWorkflowExecution(
          StartWorkflowExecutionRequest startRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        StartWorkflowExecutionWithTimeout(startRequest, resultHandler, null);
      }

      @Override
      public void StartWorkflowExecutionWithTimeout(
          StartWorkflowExecutionRequest startRequest,
          AsyncMethodCallback resultHandler,
          Long timeoutInMillis)
          throws ServiceClientException {
        forkJoinPool.execute(
            () -> {
              try {
                StartWorkflowExecutionResponse result = StartWorkflowExecution(startRequest);
                resultHandler.onComplete(result);
              } catch (TException e) {
                resultHandler.onError(e);
              }
            });
      }

      @Override
      public void StartWorkflowExecutionAsync(
          StartWorkflowExecutionAsyncRequest startRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        StartWorkflowExecutionAsyncWithTimeout(startRequest, resultHandler, null);
      }

      @SuppressWarnings("unchecked") // Generator ignores that AsyncMethodCallback is generic
      @Override
      public void GetWorkflowExecutionHistory(
          GetWorkflowExecutionHistoryRequest getRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        forkJoinPool.execute(
            () -> {
              try {
                GetWorkflowExecutionHistoryResponse result =
                    GetWorkflowExecutionHistory(getRequest);
                resultHandler.onComplete(result);
              } catch (TException e) {
                resultHandler.onError(e);
              }
            });
      }

      @SuppressWarnings("unchecked") // Generator ignores that AsyncMethodCallback is generic
      @Override
      public void GetWorkflowExecutionHistoryWithTimeout(
          GetWorkflowExecutionHistoryRequest getRequest,
          AsyncMethodCallback resultHandler,
          Long timeoutInMillis)
          throws ServiceClientException {
        GetWorkflowExecutionHistory(getRequest, resultHandler);
      }

      @Override
      public void SignalWorkflowExecution(
          SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback resultHandler)
          throws ServiceClientException {
        SignalWorkflowExecutionWithTimeout(signalRequest, resultHandler, null);
      }

      @Override
      public void SignalWorkflowExecutionWithTimeout(
          SignalWorkflowExecutionRequest signalRequest,
          AsyncMethodCallback resultHandler,
          Long timeoutInMillis)
          throws ServiceClientException {
        forkJoinPool.execute(
            () -> {
              try {
                SignalWorkflowExecution(signalRequest);
                resultHandler.onComplete(null);
              } catch (TException e) {
                resultHandler.onError(e);
              }
            });
      }

      @Override
      public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
          throws BadRequestException, InternalServiceException, EntityNotExistsException,
              ServiceBusyException, ServiceClientException {
        throw new UnsupportedOperationException("not implemented");
      }
    };
  }

  @Override
  public Future futureStub() {
    return null;
  }

  @Override
  public ClientOptions getOptions() {
    return ClientOptions.defaultInstance();
  }

  @Override
  public CompletableFuture<Boolean> isHealthy() {
    return null;
  }
}
