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

package com.uber.cadence.serviceclient;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.uber.cadence.entities.*;
import com.uber.cadence.internal.compatibility.proto.mappers.*;
import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.exceptions.BadRequestError;
import com.uber.cadence.serviceclient.exceptions.CancellationAlreadyRequestedError;
import com.uber.cadence.serviceclient.exceptions.ClientVersionNotSupportedError;
import com.uber.cadence.serviceclient.exceptions.DomainAlreadyExistsError;
import com.uber.cadence.serviceclient.exceptions.DomainNotActiveError;
import com.uber.cadence.serviceclient.exceptions.EntityNotExistsError;
import com.uber.cadence.serviceclient.exceptions.InternalServiceError;
import com.uber.cadence.serviceclient.exceptions.LimitExceededError;
import com.uber.cadence.serviceclient.exceptions.QueryFailedError;
import com.uber.cadence.serviceclient.exceptions.ServiceBusyError;
import com.uber.cadence.serviceclient.exceptions.ServiceClientError;
import com.uber.cadence.serviceclient.exceptions.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.serviceclient.exceptions.WorkflowExecutionAlreadyStartedError;
import io.grpc.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class WorkflowServiceGrpc implements IWorkflowServiceV4 {

  private final IGrpcServiceStubs grpcServiceStubs;
  private final Executor executor = MoreExecutors.directExecutor();

  WorkflowServiceGrpc(ClientOptions options) {
    this.grpcServiceStubs = IGrpcServiceStubs.newInstance(options);
  }

  @Override
  public void close() {
    grpcServiceStubs.shutdown();
  }

  @Override
  public ClientOptions getOptions() {
    return grpcServiceStubs.getOptions();
  }

  @Override
  public CompletableFuture<Boolean> isHealthy() {
    CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
    Futures.addCallback(
        grpcServiceStubs
            .metaFutureStub()
            .health(com.uber.cadence.api.v1.HealthRequest.getDefaultInstance()),
        new FutureCallback<com.uber.cadence.api.v1.HealthResponse>() {
          @Override
          public void onSuccess(com.uber.cadence.api.v1.HealthResponse response) {
            completableFuture.complete(response.getOk());
          }

          @Override
          public void onFailure(Throwable throwable) {
            completableFuture.completeExceptionally(toServiceClientException(throwable));
          }
        },
        executor);
    return completableFuture;
  }

  @Override
  public void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadlineAfter(timeoutInMillis, TimeUnit.MILLISECONDS)
            .startWorkflowExecution(RequestMapper.startWorkflowExecutionRequest(startRequest)),
        toFutureCallback(resultHandler, ResponseMapper::startWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void StartWorkflowExecutionAsyncWithTimeout(
      StartWorkflowExecutionAsyncRequest startAsyncRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadlineAfter(timeoutInMillis, TimeUnit.MILLISECONDS)
            .startWorkflowExecutionAsync(
                RequestMapper.startWorkflowExecutionAsyncRequest(startAsyncRequest)),
        toFutureCallback(resultHandler, ResponseMapper::startWorkflowExecutionAsyncResponse),
        executor);
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis)
      throws ServiceClientError {
    try {
      return ResponseMapper.getWorkflowExecutionHistoryResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .withDeadlineAfter(timeoutInMillis, TimeUnit.MILLISECONDS)
              .getWorkflowExecutionHistory(
                  RequestMapper.getWorkflowExecutionHistoryRequest(getRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadlineAfter(timeoutInMillis, TimeUnit.MILLISECONDS)
            .getWorkflowExecutionHistory(
                RequestMapper.getWorkflowExecutionHistoryRequest(getRequest)),
        toFutureCallback(resultHandler, ResponseMapper::getWorkflowExecutionHistoryResponse),
        executor);
  }

  @Override
  public void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback<Void> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .withDeadlineAfter(timeoutInMillis, TimeUnit.MILLISECONDS)
            .signalWorkflowExecution(RequestMapper.signalWorkflowExecutionRequest(signalRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RegisterDomain(RegisterDomainRequest registerRequest)
      throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      grpcServiceStubs
          .domainBlockingStub()
          .registerDomain(RequestMapper.registerDomainRequest(registerRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.describeDomainResponse(
          grpcServiceStubs
              .domainBlockingStub()
              .describeDomain(RequestMapper.describeDomainRequest(describeRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.listDomainsResponse(
          grpcServiceStubs
              .domainBlockingStub()
              .listDomains(RequestMapper.listDomainsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.updateDomainResponse(
          grpcServiceStubs
              .domainBlockingStub()
              .updateDomain(RequestMapper.updateDomainRequest(updateRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      grpcServiceStubs
          .domainBlockingStub()
          .deprecateDomain(RequestMapper.deprecateDomainRequest(deprecateRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public RestartWorkflowExecutionResponse RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest)
      throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
          EntityNotExistsError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.restartWorkflowExecutionResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .restartWorkflowExecution(
                  RequestMapper.restartWorkflowExecutionRequest(restartRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.startWorkflowExecutionResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .startWorkflowExecution(RequestMapper.startWorkflowExecutionRequest(startRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.startWorkflowExecutionAsyncResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .startWorkflowExecutionAsync(
                  RequestMapper.startWorkflowExecutionAsyncRequest(startRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.getWorkflowExecutionHistoryResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .getWorkflowExecutionHistory(
                  RequestMapper.getWorkflowExecutionHistoryRequest(getRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.pollForDecisionTaskResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .pollForDecisionTask(RequestMapper.pollForDecisionTaskRequest(pollRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      return ResponseMapper.respondDecisionTaskCompletedResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .respondDecisionTaskCompleted(
                  RequestMapper.respondDecisionTaskCompletedRequest(completeRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondDecisionTaskFailed(RequestMapper.respondDecisionTaskFailedRequest(failedRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.pollForActivityTaskResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .pollForActivityTask(RequestMapper.pollForActivityTaskRequest(pollRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      return ResponseMapper.recordActivityTaskHeartbeatResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .recordActivityTaskHeartbeat(
                  RequestMapper.recordActivityTaskHeartbeatRequest(heartbeatRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      return ResponseMapper.recordActivityTaskHeartbeatResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .recordActivityTaskHeartbeatByID(
                  RequestMapper.recordActivityTaskHeartbeatByIDRequest(heartbeatRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskCompleted(
              RequestMapper.respondActivityTaskCompletedRequest(completeRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskCompletedByID(
              RequestMapper.respondActivityTaskCompletedByIDRequest(completeRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskFailed(RequestMapper.respondActivityTaskFailedRequest(failRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskFailedByID(
              RequestMapper.respondActivityTaskFailedByIDRequest(failRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskCanceled(
              RequestMapper.respondActivityTaskCanceledRequest(canceledRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondActivityTaskCanceledByID(
              RequestMapper.respondActivityTaskCanceledByIDRequest(canceledRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError,
          ServiceBusyError, DomainNotActiveError, LimitExceededError,
          ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .requestCancelWorkflowExecution(
              RequestMapper.requestCancelWorkflowExecutionRequest(cancelRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError {
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .signalWorkflowExecution(RequestMapper.signalWorkflowExecutionRequest(signalRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError,
          ServiceClientError {
    try {
      return ResponseMapper.signalWithStartWorkflowExecutionResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .signalWithStartWorkflowExecution(
                  RequestMapper.signalWithStartWorkflowExecutionRequest(signalWithStartRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.signalWithStartWorkflowExecutionAsyncResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .signalWithStartWorkflowExecutionAsync(
                  RequestMapper.signalWithStartWorkflowExecutionAsyncRequest(
                      signalWithStartRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ResetWorkflowExecutionResponse ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.resetWorkflowExecutionResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .resetWorkflowExecution(RequestMapper.resetWorkflowExecutionRequest(resetRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError {
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .terminateWorkflowExecution(
              RequestMapper.terminateWorkflowExecutionRequest(terminateRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.listOpenWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .listOpenWorkflowExecutions(
                  RequestMapper.listOpenWorkflowExecutionsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.listClosedWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .listClosedWorkflowExecutions(
                  RequestMapper.listClosedWorkflowExecutionsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.listWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .listWorkflowExecutions(RequestMapper.listWorkflowExecutionsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.listArchivedWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .listArchivedWorkflowExecutions(
                  RequestMapper.listArchivedWorkflowExecutionsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.scanWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .scanWorkflowExecutions(RequestMapper.scanWorkflowExecutionsRequest(listRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.countWorkflowExecutionsResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .countWorkflowExecutions(RequestMapper.countWorkflowExecutionsRequest(countRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public GetSearchAttributesResponse GetSearchAttributes()
      throws ServiceBusyError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.getSearchAttributesResponse(
          grpcServiceStubs
              .visibilityBlockingStub()
              .getSearchAttributes(
                  com.uber.cadence.api.v1.GetSearchAttributesRequest.newBuilder().build()));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      grpcServiceStubs
          .workerBlockingStub()
          .respondQueryTaskCompleted(
              RequestMapper.respondQueryTaskCompletedRequest(completeRequest));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError {
    try {
      return ResponseMapper.resetStickyTaskListResponse(
          grpcServiceStubs
              .workerBlockingStub()
              .resetStickyTaskList(RequestMapper.resetStickyTaskListRequest(resetRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
      throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.queryWorkflowResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .queryWorkflow(RequestMapper.queryWorkflowRequest(queryRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.describeWorkflowExecutionResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .describeWorkflowExecution(
                  RequestMapper.describeWorkflowExecutionRequest(describeRequest)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.describeTaskListResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .describeTaskList(RequestMapper.describeTaskListRequest(request)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ClusterInfo GetClusterInfo()
      throws InternalServiceError, ServiceBusyError, ServiceClientError {
    try {
      return ResponseMapper.clusterInfoResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .getClusterInfo(com.uber.cadence.api.v1.GetClusterInfoRequest.getDefaultInstance()));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError {
    try {
      return ResponseMapper.getTaskListsByDomainResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .getTaskListsByDomain(RequestMapper.getTaskListsByDomainRequest(request)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public ListTaskListPartitionsResponse ListTaskListPartitions(
      ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ServiceClientError {
    try {
      return ResponseMapper.listTaskListPartitionsResponse(
          grpcServiceStubs
              .workflowBlockingStub()
              .listTaskListPartitions(RequestMapper.listTaskListPartitionsRequest(request)));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          ServiceClientError {
    try {
      grpcServiceStubs
          .workflowBlockingStub()
          .refreshWorkflowTasks(RequestMapper.refreshWorkflowTasksRequest(request));
    } catch (Exception e) {
      throw toServiceClientException(e);
    }
  }

  @Override
  public void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .domainFutureStub()
            .registerDomain(RequestMapper.registerDomainRequest(registerRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void DescribeDomain(
      DescribeDomainRequest describeRequest,
      AsyncMethodCallback<DescribeDomainResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .domainFutureStub()
            .describeDomain(RequestMapper.describeDomainRequest(describeRequest)),
        toFutureCallback(resultHandler, ResponseMapper::describeDomainResponse),
        executor);
  }

  @Override
  public void ListDomains(
      ListDomainsRequest listRequest, AsyncMethodCallback<ListDomainsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .domainFutureStub()
            .listDomains(RequestMapper.listDomainsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::listDomainsResponse),
        executor);
  }

  @Override
  public void UpdateDomain(
      UpdateDomainRequest updateRequest, AsyncMethodCallback<UpdateDomainResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .domainFutureStub()
            .updateDomain(RequestMapper.updateDomainRequest(updateRequest)),
        toFutureCallback(resultHandler, ResponseMapper::updateDomainResponse),
        executor);
  }

  @Override
  public void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .domainFutureStub()
            .deprecateDomain(RequestMapper.deprecateDomainRequest(deprecateRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest,
      AsyncMethodCallback<RestartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .restartWorkflowExecution(
                RequestMapper.restartWorkflowExecutionRequest(restartRequest)),
        toFutureCallback(resultHandler, ResponseMapper::restartWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .startWorkflowExecution(RequestMapper.startWorkflowExecutionRequest(startRequest)),
        toFutureCallback(resultHandler, ResponseMapper::startWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .startWorkflowExecutionAsync(
                RequestMapper.startWorkflowExecutionAsyncRequest(startRequest)),
        toFutureCallback(resultHandler, ResponseMapper::startWorkflowExecutionAsyncResponse),
        executor);
  }

  @Override
  public void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .getWorkflowExecutionHistory(
                RequestMapper.getWorkflowExecutionHistoryRequest(getRequest)),
        toFutureCallback(resultHandler, ResponseMapper::getWorkflowExecutionHistoryResponse),
        executor);
  }

  @Override
  public void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest,
      AsyncMethodCallback<PollForDecisionTaskResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .pollForDecisionTask(RequestMapper.pollForDecisionTaskRequest(pollRequest)),
        toFutureCallback(resultHandler, ResponseMapper::pollForDecisionTaskResponse),
        executor);
  }

  @Override
  public void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest,
      AsyncMethodCallback<RespondDecisionTaskCompletedResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondDecisionTaskCompleted(
                RequestMapper.respondDecisionTaskCompletedRequest(completeRequest)),
        toFutureCallback(resultHandler, ResponseMapper::respondDecisionTaskCompletedResponse),
        executor);
  }

  @Override
  public void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondDecisionTaskFailed(
                RequestMapper.respondDecisionTaskFailedRequest(failedRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void PollForActivityTask(
      PollForActivityTaskRequest pollRequest,
      AsyncMethodCallback<PollForActivityTaskResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .pollForActivityTask(RequestMapper.pollForActivityTaskRequest(pollRequest)),
        toFutureCallback(resultHandler, ResponseMapper::pollForActivityTaskResponse),
        executor);
  }

  @Override
  public void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .recordActivityTaskHeartbeat(
                RequestMapper.recordActivityTaskHeartbeatRequest(heartbeatRequest)),
        toFutureCallback(resultHandler, ResponseMapper::recordActivityTaskHeartbeatResponse),
        executor);
  }

  @Override
  public void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .recordActivityTaskHeartbeatByID(
                RequestMapper.recordActivityTaskHeartbeatByIDRequest(heartbeatRequest)),
        toFutureCallback(resultHandler, ResponseMapper::recordActivityTaskHeartbeatResponse),
        executor);
  }

  @Override
  public void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskCompleted(
                RequestMapper.respondActivityTaskCompletedRequest(completeRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskCompletedByID(
                RequestMapper.respondActivityTaskCompletedByIDRequest(completeRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskFailed(RequestMapper.respondActivityTaskFailedRequest(failRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskFailedByID(
                RequestMapper.respondActivityTaskFailedByIDRequest(failRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskCanceled(
                RequestMapper.respondActivityTaskCanceledRequest(canceledRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondActivityTaskCanceledByID(
                RequestMapper.respondActivityTaskCanceledByIDRequest(canceledRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .requestCancelWorkflowExecution(
                RequestMapper.requestCancelWorkflowExecutionRequest(cancelRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .signalWorkflowExecution(RequestMapper.signalWorkflowExecutionRequest(signalRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .signalWithStartWorkflowExecution(
                RequestMapper.signalWithStartWorkflowExecutionRequest(signalWithStartRequest)),
        toFutureCallback(resultHandler, ResponseMapper::signalWithStartWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
      AsyncMethodCallback<SignalWithStartWorkflowExecutionAsyncResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .signalWithStartWorkflowExecutionAsync(
                RequestMapper.signalWithStartWorkflowExecutionAsyncRequest(signalWithStartRequest)),
        toFutureCallback(
            resultHandler, ResponseMapper::signalWithStartWorkflowExecutionAsyncResponse),
        executor);
  }

  @Override
  public void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest,
      AsyncMethodCallback<ResetWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .resetWorkflowExecution(RequestMapper.resetWorkflowExecutionRequest(resetRequest)),
        toFutureCallback(resultHandler, ResponseMapper::resetWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .terminateWorkflowExecution(
                RequestMapper.terminateWorkflowExecutionRequest(terminateRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListOpenWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .listOpenWorkflowExecutions(
                RequestMapper.listOpenWorkflowExecutionsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::listOpenWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListClosedWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .listClosedWorkflowExecutions(
                RequestMapper.listClosedWorkflowExecutionsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::listClosedWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .listWorkflowExecutions(RequestMapper.listWorkflowExecutionsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::listWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListArchivedWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .listArchivedWorkflowExecutions(
                RequestMapper.listArchivedWorkflowExecutionsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::listArchivedWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .scanWorkflowExecutions(RequestMapper.scanWorkflowExecutionsRequest(listRequest)),
        toFutureCallback(resultHandler, ResponseMapper::scanWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest,
      AsyncMethodCallback<CountWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .countWorkflowExecutions(RequestMapper.countWorkflowExecutionsRequest(countRequest)),
        toFutureCallback(resultHandler, ResponseMapper::countWorkflowExecutionsResponse),
        executor);
  }

  @Override
  public void GetSearchAttributes(AsyncMethodCallback<GetSearchAttributesResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .visibilityFutureStub()
            .getSearchAttributes(
                com.uber.cadence.api.v1.GetSearchAttributesRequest.getDefaultInstance()),
        toFutureCallback(resultHandler, ResponseMapper::getSearchAttributesResponse),
        executor);
  }

  @Override
  public void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .respondQueryTaskCompleted(
                RequestMapper.respondQueryTaskCompletedRequest(completeRequest)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  @Override
  public void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest,
      AsyncMethodCallback<ResetStickyTaskListResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workerFutureStub()
            .resetStickyTaskList(RequestMapper.resetStickyTaskListRequest(resetRequest)),
        toFutureCallback(resultHandler, ResponseMapper::resetStickyTaskListResponse),
        executor);
  }

  @Override
  public void QueryWorkflow(
      QueryWorkflowRequest queryRequest, AsyncMethodCallback<QueryWorkflowResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .queryWorkflow(RequestMapper.queryWorkflowRequest(queryRequest)),
        toFutureCallback(resultHandler, ResponseMapper::queryWorkflowResponse),
        executor);
  }

  @Override
  public void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest,
      AsyncMethodCallback<DescribeWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .describeWorkflowExecution(
                RequestMapper.describeWorkflowExecutionRequest(describeRequest)),
        toFutureCallback(resultHandler, ResponseMapper::describeWorkflowExecutionResponse),
        executor);
  }

  @Override
  public void DescribeTaskList(
      DescribeTaskListRequest request, AsyncMethodCallback<DescribeTaskListResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .describeTaskList(RequestMapper.describeTaskListRequest(request)),
        toFutureCallback(resultHandler, ResponseMapper::describeTaskListResponse),
        executor);
  }

  @Override
  public void GetClusterInfo(AsyncMethodCallback<ClusterInfo> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .getClusterInfo(com.uber.cadence.api.v1.GetClusterInfoRequest.getDefaultInstance()),
        toFutureCallback(resultHandler, ResponseMapper::getClusterInfoResponse),
        executor);
  }

  @Override
  public void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request,
      AsyncMethodCallback<GetTaskListsByDomainResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .getTaskListsByDomain(RequestMapper.getTaskListsByDomainRequest(request)),
        toFutureCallback(resultHandler, ResponseMapper::getTaskListsByDomainResponse),
        executor);
  }

  @Override
  public void ListTaskListPartitions(
      ListTaskListPartitionsRequest request,
      AsyncMethodCallback<ListTaskListPartitionsResponse> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .listTaskListPartitions(RequestMapper.listTaskListPartitionsRequest(request)),
        toFutureCallback(resultHandler, ResponseMapper::listTaskListPartitionsResponse),
        executor);
  }

  @Override
  public void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError {
    Futures.addCallback(
        grpcServiceStubs
            .workflowFutureStub()
            .refreshWorkflowTasks(RequestMapper.refreshWorkflowTasksRequest(request)),
        toFutureCallback(resultHandler, r -> null),
        executor);
  }

  private ServiceClientError toServiceClientException(Throwable t) {
    if (t instanceof ServiceClientError) {
      return (ServiceClientError) t;
    } else if (t instanceof StatusRuntimeException) {
      return ErrorMapper.Error((StatusRuntimeException) t);
    } else {
      return new ServiceClientError(t);
    }
  }

  private <T, R> FutureCallback<R> toFutureCallback(
      AsyncMethodCallback<T> resultHandler, Function<R, T> mapper) {
    return new FutureCallback<R>() {
      @Override
      public void onSuccess(R t) {
        resultHandler.onComplete(mapper.apply(t));
      }

      @Override
      public void onFailure(Throwable throwable) {
        resultHandler.onError(toServiceClientException(throwable));
      }
    };
  }
}
