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

import com.uber.cadence.*;
import java.util.concurrent.CompletableFuture;

public interface IWorkflowService extends Iface, AsyncIface {
  void close();

  ClientOptions getOptions();

  /**
   * StartWorkflowExecutionWithTimeout start workflow same as StartWorkflowExecution but with
   * timeout
   *
   * @param startRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws CadenceError
   */
  void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler,
      Long timeoutInMillis)
      throws CadenceError;

  /**
   * StartWorkflowExecutionAsyncWithTimeout start workflow same as StartWorkflowExecutionAsync but
   * with timeout
   *
   * @param startAsyncRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws CadenceError
   */
  void StartWorkflowExecutionAsyncWithTimeout(
      StartWorkflowExecutionAsyncRequest startAsyncRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler,
      Long timeoutInMillis)
      throws CadenceError;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history same as GetWorkflowExecutionHistory
   * but with timeout.
   *
   * @param getRequest
   * @param timeoutInMillis
   * @return GetWorkflowExecutionHistoryResponse
   * @throws CadenceError
   */
  GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis) throws CadenceError;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history asynchronously same as
   * GetWorkflowExecutionHistory but with timeout.
   *
   * @param getRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws CadenceError
   */
  void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler,
      Long timeoutInMillis)
      throws CadenceError;

  /**
   * SignalWorkflowExecutionWithTimeout signal workflow same as SignalWorkflowExecution but with
   * timeout
   *
   * @param signalRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws CadenceError
   */
  void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback<Void> resultHandler,
      Long timeoutInMillis)
      throws CadenceError;

  /**
   * Checks if we have a valid connection to the Cadence cluster, and potentially resets the peer
   * list
   */
  CompletableFuture<Boolean> isHealthy();
}

interface Iface {

  /**
   * RegisterDomain creates a new domain which can be used as a container for all resources. Domain
   * is a top level entity within Cadence, used as a container for all resources like workflow
   * executions, tasklists, etc. Domain acts as a sandbox and provides isolation for all resources
   * within the domain. All resources belongs to exactly one domain.
   *
   * @param registerRequest
   */
  void RegisterDomain(RegisterDomainRequest registerRequest)
      throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * DescribeDomain returns the information and configuration for a registered domain.
   *
   * @param describeRequest
   */
  DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  DiagnoseWorkflowExecutionResponse DiagnoseWorkflowExecution(
      DiagnoseWorkflowExecutionRequest diagnoseRequest)
      throws DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ListDomains returns the information and configuration for all domains.
   *
   * @param listRequest
   */
  ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * UpdateDomain is used to update the information and configuration for a registered domain.
   *
   * @param updateRequest
   */
  UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * DeprecateDomain us used to update status of a registered domain to DEPRECATED. Once the domain
   * is deprecated it cannot be used to start new workflow executions. Existing workflow executions
   * will continue to run on deprecated domains.
   *
   * @param deprecateRequest
   */
  void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * RestartWorkflowExecution restarts a previous workflow If the workflow is currently running it
   * will terminate and restart
   *
   * @param restartRequest
   */
  RestartWorkflowExecutionResponse RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest)
      throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
          EntityNotExistsError, ClientVersionNotSupportedError, CadenceError;

  /**
   * StartWorkflowExecution starts a new long running workflow instance. It will create the instance
   * with 'WorkflowExecutionStarted' event in history and also schedule the first DecisionTask for
   * the worker to make the first decision for this instance. It will return
   * 'WorkflowExecutionAlreadyStartedError', if an instance already exists with same workflowId.
   *
   * @param startRequest
   */
  StartWorkflowExecutionResponse StartWorkflowExecution(StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * StartWorkflowExecutionAsync starts a new long running workflow instance asynchronously. It will
   * push a StartWorkflowExecutionRequest to a queue and immediately return a response. The request
   * will be processed by a separate consumer eventually.
   *
   * @param startRequest
   */
  StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * Returns the history of specified workflow execution. It fails with 'EntityNotExistError' if
   * speficied workflow execution in unknown to the service.
   *
   * @param getRequest
   */
  GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * PollForDecisionTask is called by application worker to process DecisionTask from a specific
   * taskList. A DecisionTask is dispatched to callers for active workflow executions, with pending
   * decisions. Application is then expected to call 'RespondDecisionTaskCompleted' API when it is
   * done processing the DecisionTask. It will also create a 'DecisionTaskStarted' event in the
   * history for that session before handing off DecisionTask to application worker.
   *
   * @param pollRequest
   */
  PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, CadenceError;

  /**
   * RespondDecisionTaskCompleted is called by application worker to complete a DecisionTask handed
   * as a result of 'PollForDecisionTask' API call. Completing a DecisionTask will result in new
   * events for the workflow execution and potentially new ActivityTask being created for
   * corresponding decisions. It will also create a DecisionTaskCompleted event in the history for
   * that session. Use the 'taskToken' provided as response of PollForDecisionTask API call for
   * completing the DecisionTask. The response could contain a new decision task if there is one or
   * if the request asking for one.
   *
   * @param completeRequest
   */
  RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondDecisionTaskFailed is called by application worker to indicate failure. This results in
   * DecisionTaskFailedEvent written to the history and a new DecisionTask created. This API can be
   * used by client to either clear sticky tasklist or report any panics during DecisionTask
   * processing. Cadence will only append first DecisionTaskFailed event to the history of workflow
   * execution for consecutive failures.
   *
   * @param failedRequest
   */
  void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * PollForActivityTask is called by application worker to process ActivityTask from a specific
   * taskList. ActivityTask is dispatched to callers whenever a ScheduleTask decision is made for a
   * workflow execution. Application is expected to call 'RespondActivityTaskCompleted' or
   * 'RespondActivityTaskFailed' once it is done processing the task. Application also needs to call
   * 'RecordActivityTaskHeartbeat' API within 'heartbeatTimeoutSeconds' interval to prevent the task
   * from getting timed out. An event 'ActivityTaskStarted' event is also written to workflow
   * execution history before the ActivityTask is dispatched to application worker.
   *
   * @param pollRequest
   */
  PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, CadenceError;

  /**
   * RecordActivityTaskHeartbeat is called by application worker while it is processing an
   * ActivityTask. If worker fails to heartbeat within 'heartbeatTimeoutSeconds' interval for the
   * ActivityTask, then it will be marked as timedout and 'ActivityTaskTimedOut' event will be
   * written to the workflow history. Calling 'RecordActivityTaskHeartbeat' will fail with
   * 'EntityNotExistsError' in such situations. Use the 'taskToken' provided as response of
   * PollForActivityTask API call for heartbeating.
   *
   * @param heartbeatRequest
   */
  RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RecordActivityTaskHeartbeatByID is called by application worker while it is processing an
   * ActivityTask. If worker fails to heartbeat within 'heartbeatTimeoutSeconds' interval for the
   * ActivityTask, then it will be marked as timedout and 'ActivityTaskTimedOut' event will be
   * written to the workflow history. Calling 'RecordActivityTaskHeartbeatByID' will fail with
   * 'EntityNotExistsError' in such situations. Instead of using 'taskToken' like in
   * RecordActivityTaskHeartbeat, use Domain, WorkflowID and ActivityID
   *
   * @param heartbeatRequest
   */
  RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskCompleted is called by application worker when it is done processing an
   * ActivityTask. It will result in a new 'ActivityTaskCompleted' event being written to the
   * workflow history and a new DecisionTask created for the workflow so new decisions could be
   * made. Use the 'taskToken' provided as response of PollForActivityTask API call for completion.
   * It fails with 'EntityNotExistsError' if the taskToken is not valid anymore due to activity
   * timeout.
   *
   * @param completeRequest
   */
  void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskCompletedByID is called by application worker when it is done processing an
   * ActivityTask. It will result in a new 'ActivityTaskCompleted' event being written to the
   * workflow history and a new DecisionTask created for the workflow so new decisions could be
   * made. Similar to RespondActivityTaskCompleted but use Domain, WorkflowID and ActivityID instead
   * of 'taskToken' for completion. It fails with 'EntityNotExistsError' if the these IDs are not
   * valid anymore due to activity timeout.
   *
   * @param completeRequest
   */
  void RespondActivityTaskCompletedByID(RespondActivityTaskCompletedByIDRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskFailed is called by application worker when it is done processing an
   * ActivityTask. It will result in a new 'ActivityTaskFailed' event being written to the workflow
   * history and a new DecisionTask created for the workflow instance so new decisions could be
   * made. Use the 'taskToken' provided as response of PollForActivityTask API call for completion.
   * It fails with 'EntityNotExistsError' if the taskToken is not valid anymore due to activity
   * timeout.
   *
   * @param failRequest
   */
  void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskFailedByID is called by application worker when it is done processing an
   * ActivityTask. It will result in a new 'ActivityTaskFailed' event being written to the workflow
   * history and a new DecisionTask created for the workflow instance so new decisions could be
   * made. Similar to RespondActivityTaskFailed but use Domain, WorkflowID and ActivityID instead of
   * 'taskToken' for completion. It fails with 'EntityNotExistsError' if the these IDs are not valid
   * anymore due to activity timeout.
   *
   * @param failRequest
   */
  void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskCanceled is called by application worker when it is successfully canceled an
   * ActivityTask. It will result in a new 'ActivityTaskCanceled' event being written to the
   * workflow history and a new DecisionTask created for the workflow instance so new decisions
   * could be made. Use the 'taskToken' provided as response of PollForActivityTask API call for
   * completion. It fails with 'EntityNotExistsError' if the taskToken is not valid anymore due to
   * activity timeout.
   *
   * @param canceledRequest
   */
  void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RespondActivityTaskCanceledByID is called by application worker when it is successfully
   * canceled an ActivityTask. It will result in a new 'ActivityTaskCanceled' event being written to
   * the workflow history and a new DecisionTask created for the workflow instance so new decisions
   * could be made. Similar to RespondActivityTaskCanceled but use Domain, WorkflowID and ActivityID
   * instead of 'taskToken' for completion. It fails with 'EntityNotExistsError' if the these IDs
   * are not valid anymore due to activity timeout.
   *
   * @param canceledRequest
   */
  void RespondActivityTaskCanceledByID(RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          CadenceError;

  /**
   * RequestCancelWorkflowExecution is called by application worker when it wants to request
   * cancellation of a workflow instance. It will result in a new 'WorkflowExecutionCancelRequested'
   * event being written to the workflow history and a new DecisionTask created for the workflow
   * instance so new decisions could be made. It fails with 'EntityNotExistsError' if the workflow
   * is not valid anymore due to completion or doesn't exist.
   *
   * @param cancelRequest
   */
  void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError,
          ServiceBusyError, DomainNotActiveError, LimitExceededError,
          ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError, CadenceError;

  /**
   * SignalWorkflowExecution is used to send a signal event to running workflow execution. This
   * results in WorkflowExecutionSignaled event recorded in the history and a decision task being
   * created for the execution.
   *
   * @param signalRequest
   */
  void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, CadenceError;

  /**
   * SignalWithStartWorkflowExecution is used to ensure sending signal to a workflow. If the
   * workflow is running, this results in WorkflowExecutionSignaled event being recorded in the
   * history and a decision task being created for the execution. If the workflow is not running or
   * not found, this results in WorkflowExecutionStarted and WorkflowExecutionSignaled events being
   * recorded in history, and a decision task being created for the execution
   *
   * @param signalWithStartRequest
   */
  StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError,
          CadenceError;

  /**
   * SignalWithStartWorkflowExecutionAsync is used to ensure sending signal to a workflow
   * asynchronously. It will push a SignalWithStartWorkflowExecutionRequest to a queue and
   * immediately return a response. The request will be processed by a separate consumer eventually.
   *
   * @param signalWithStartRequest
   */
  SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ResetWorkflowExecution reset an existing workflow execution to DecisionTaskCompleted
   * event(exclusive). And it will immediately terminating the current execution instance.
   *
   * @param resetRequest
   */
  ResetWorkflowExecutionResponse ResetWorkflowExecution(ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError, CadenceError;

  /**
   * TerminateWorkflowExecution terminates an existing workflow execution by recording
   * WorkflowExecutionTerminated event in the history and immediately terminating the execution
   * instance.
   *
   * @param terminateRequest
   */
  void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, CadenceError;

  /**
   * ListOpenWorkflowExecutions is a visibility API to list the open executions in a specific
   * domain.
   *
   * @param listRequest
   */
  ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ListClosedWorkflowExecutions is a visibility API to list the closed executions in a specific
   * domain.
   *
   * @param listRequest
   */
  ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ListWorkflowExecutions is a visibility API to list workflow executions in a specific domain.
   *
   * @param listRequest
   */
  ListWorkflowExecutionsResponse ListWorkflowExecutions(ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ListArchivedWorkflowExecutions is a visibility API to list archived workflow executions in a
   * specific domain.
   *
   * @param listRequest
   */
  ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ScanWorkflowExecutions is a visibility API to list large amount of workflow executions in a
   * specific domain without order.
   *
   * @param listRequest
   */
  ListWorkflowExecutionsResponse ScanWorkflowExecutions(ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * CountWorkflowExecutions is a visibility API to count of workflow executions in a specific
   * domain.
   *
   * @param countRequest
   */
  CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * GetSearchAttributes is a visibility API to get all legal keys that could be used in list APIs
   */
  GetSearchAttributesResponse GetSearchAttributes()
      throws ServiceBusyError, ClientVersionNotSupportedError, CadenceError;

  /**
   * RespondQueryTaskCompleted is called by application worker to complete a QueryTask (which is a
   * DecisionTask for query) as a result of 'PollForDecisionTask' API call. Completing a QueryTask
   * will unblock the client call to 'QueryWorkflow' API and return the query result to client as a
   * response to 'QueryWorkflow' API call.
   *
   * @param completeRequest
   */
  void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError, CadenceError;

  /**
   * Reset the sticky tasklist related information in mutable state of a given workflow. Things
   * cleared are: 1. StickyTaskList 2. StickyScheduleToStartTimeout 3. ClientLibraryVersion 4.
   * ClientFeatureVersion 5. ClientImpl
   *
   * @param resetRequest
   */
  ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, CadenceError;

  /**
   * QueryWorkflow returns query result for a specified workflow execution
   *
   * @param queryRequest
   */
  QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
      throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, CadenceError;

  /**
   * DescribeWorkflowExecution returns information about the specified workflow execution.
   *
   * @param describeRequest
   */
  DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * DescribeTaskList returns information about the target tasklist, right now this API returns the
   * pollers which polled this tasklist in last few minutes.
   *
   * @param request
   */
  DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /** GetClusterInfo returns information about cadence cluster */
  ClusterInfo GetClusterInfo() throws InternalServiceError, ServiceBusyError, CadenceError;

  /**
   * GetTaskListsByDomain returns the list of all the task lists for a domainName.
   *
   * @param request
   */
  GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, CadenceError;

  /**
   * ReapplyEvents applies stale events to the current workflow and current run
   *
   * @param request
   */
  ListTaskListPartitionsResponse ListTaskListPartitions(ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          CadenceError;

  /**
   * RefreshWorkflowTasks refreshes all tasks of a workflow
   *
   * @param request
   */
  void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          CadenceError;
}

interface AsyncIface {

  void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void DescribeDomain(
      DescribeDomainRequest describeRequest,
      AsyncMethodCallback<DescribeDomainResponse> resultHandler)
      throws CadenceError;

  void DiagnoseWorkflowExecution(
      DiagnoseWorkflowExecutionRequest diagnoseRequest, AsyncMethodCallback resultHandler)
      throws CadenceError;

  void ListDomains(
      ListDomainsRequest listRequest, AsyncMethodCallback<ListDomainsResponse> resultHandler)
      throws CadenceError;

  void UpdateDomain(
      UpdateDomainRequest updateRequest, AsyncMethodCallback<UpdateDomainResponse> resultHandler)
      throws CadenceError;

  void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest,
      AsyncMethodCallback<RestartWorkflowExecutionResponse> resultHandler)
      throws CadenceError;

  void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws CadenceError;

  void StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler)
      throws CadenceError;

  void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler)
      throws CadenceError;

  void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest,
      AsyncMethodCallback<PollForDecisionTaskResponse> resultHandler)
      throws CadenceError;

  void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest,
      AsyncMethodCallback<RespondDecisionTaskCompletedResponse> resultHandler)
      throws CadenceError;

  void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void PollForActivityTask(
      PollForActivityTaskRequest pollRequest,
      AsyncMethodCallback<PollForActivityTaskResponse> resultHandler)
      throws CadenceError;

  void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws CadenceError;

  void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws CadenceError;

  void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws CadenceError;

  void SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
      AsyncMethodCallback<SignalWithStartWorkflowExecutionAsyncResponse> resultHandler)
      throws CadenceError;

  void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest,
      AsyncMethodCallback<ResetWorkflowExecutionResponse> resultHandler)
      throws CadenceError;

  void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListOpenWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListClosedWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListArchivedWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest,
      AsyncMethodCallback<CountWorkflowExecutionsResponse> resultHandler)
      throws CadenceError;

  void GetSearchAttributes(AsyncMethodCallback<GetSearchAttributesResponse> resultHandler)
      throws CadenceError;

  void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;

  void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest,
      AsyncMethodCallback<ResetStickyTaskListResponse> resultHandler)
      throws CadenceError;

  void QueryWorkflow(
      QueryWorkflowRequest queryRequest, AsyncMethodCallback<QueryWorkflowResponse> resultHandler)
      throws CadenceError;

  void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest,
      AsyncMethodCallback<DescribeWorkflowExecutionResponse> resultHandler)
      throws CadenceError;

  void DescribeTaskList(
      DescribeTaskListRequest request, AsyncMethodCallback<DescribeTaskListResponse> resultHandler)
      throws CadenceError;

  void GetClusterInfo(AsyncMethodCallback<ClusterInfo> resultHandler) throws CadenceError;

  void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request,
      AsyncMethodCallback<GetTaskListsByDomainResponse> resultHandler)
      throws CadenceError;

  void ListTaskListPartitions(
      ListTaskListPartitionsRequest request,
      AsyncMethodCallback<ListTaskListPartitionsResponse> resultHandler)
      throws CadenceError;

  void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback<Void> resultHandler)
      throws CadenceError;
}
