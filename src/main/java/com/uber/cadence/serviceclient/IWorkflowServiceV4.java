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

import com.uber.cadence.entities.*;
import com.uber.cadence.serviceclient.exceptions.*;
import java.util.concurrent.CompletableFuture;

public interface IWorkflowServiceV4 extends Iface, AsyncIface {
  void close();

  ClientOptions getOptions();

  /**
   * StartWorkflowExecutionWithTimeout start workflow same as StartWorkflowExecution but with
   * timeout
   *
   * @param startRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws ServiceClientError
   */
  void StartWorkflowExecutionWithTimeout(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError;

  /**
   * StartWorkflowExecutionAsyncWithTimeout start workflow same as StartWorkflowExecutionAsync but
   * with timeout
   *
   * @param startAsyncRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws ServiceClientError
   */
  void StartWorkflowExecutionAsyncWithTimeout(
      StartWorkflowExecutionAsyncRequest startAsyncRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history same as GetWorkflowExecutionHistory
   * but with timeout.
   *
   * @param getRequest
   * @param timeoutInMillis
   * @return GetWorkflowExecutionHistoryResponse
   * @throws ServiceClientError
   */
  GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest, Long timeoutInMillis)
      throws ServiceClientError;

  /**
   * GetWorkflowExecutionHistoryWithTimeout get workflow history asynchronously same as
   * GetWorkflowExecutionHistory but with timeout.
   *
   * @param getRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws ServiceClientError
   */
  void GetWorkflowExecutionHistoryWithTimeout(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError;

  /**
   * SignalWorkflowExecutionWithTimeout signal workflow same as SignalWorkflowExecution but with
   * timeout
   *
   * @param signalRequest
   * @param resultHandler
   * @param timeoutInMillis
   * @throws ServiceClientError
   */
  void SignalWorkflowExecutionWithTimeout(
      SignalWorkflowExecutionRequest signalRequest,
      AsyncMethodCallback<Void> resultHandler,
      Long timeoutInMillis)
      throws ServiceClientError;

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
  public void RegisterDomain(RegisterDomainRequest registerRequest)
      throws BadRequestError, DomainAlreadyExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * DescribeDomain returns the information and configuration for a registered domain.
   *
   * @param describeRequest
   */
  public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ListDomains returns the information and configuration for all domains.
   *
   * @param listRequest
   */
  public ListDomainsResponse ListDomains(ListDomainsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * UpdateDomain is used to update the information and configuration for a registered domain.
   *
   * @param updateRequest
   */
  public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * DeprecateDomain us used to update status of a registered domain to DEPRECATED. Once the domain
   * is deprecated it cannot be used to start new workflow executions. Existing workflow executions
   * will continue to run on deprecated domains.
   *
   * @param deprecateRequest
   */
  public void DeprecateDomain(DeprecateDomainRequest deprecateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * RestartWorkflowExecution restarts a previous workflow If the workflow is currently running it
   * will terminate and restart
   *
   * @param restartRequest
   */
  public RestartWorkflowExecutionResponse RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest)
      throws BadRequestError, ServiceBusyError, DomainNotActiveError, LimitExceededError,
          EntityNotExistsError, ClientVersionNotSupportedError, ServiceClientError;

  /**
   * StartWorkflowExecution starts a new long running workflow instance. It will create the instance
   * with 'WorkflowExecutionStarted' event in history and also schedule the first DecisionTask for
   * the worker to make the first decision for this instance. It will return
   * 'WorkflowExecutionAlreadyStartedError', if an instance already exists with same workflowId.
   *
   * @param startRequest
   */
  public StartWorkflowExecutionResponse StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * StartWorkflowExecutionAsync starts a new long running workflow instance asynchronously. It will
   * push a StartWorkflowExecutionRequest to a queue and immediately return a response. The request
   * will be processed by a separate consumer eventually.
   *
   * @param startRequest
   */
  public StartWorkflowExecutionAsyncResponse StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * Returns the history of specified workflow execution. It fails with 'EntityNotExistError' if
   * speficied workflow execution in unknown to the service.
   *
   * @param getRequest
   */
  public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * PollForDecisionTask is called by application worker to process DecisionTask from a specific
   * taskList. A DecisionTask is dispatched to callers for active workflow executions, with pending
   * decisions. Application is then expected to call 'RespondDecisionTaskCompleted' API when it is
   * done processing the DecisionTask. It will also create a 'DecisionTaskStarted' event in the
   * history for that session before handing off DecisionTask to application worker.
   *
   * @param pollRequest
   */
  public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError;

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
  public RespondDecisionTaskCompletedResponse RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

  /**
   * RespondDecisionTaskFailed is called by application worker to indicate failure. This results in
   * DecisionTaskFailedEvent written to the history and a new DecisionTask created. This API can be
   * used by client to either clear sticky tasklist or report any panics during DecisionTask
   * processing. Cadence will only append first DecisionTaskFailed event to the history of workflow
   * execution for consecutive failures.
   *
   * @param failedRequest
   */
  public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest)
      throws BadRequestError, ServiceBusyError, LimitExceededError, EntityNotExistsError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError;

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
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

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
  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws BadRequestError, EntityNotExistsError, DomainNotActiveError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

  /**
   * RequestCancelWorkflowExecution is called by application worker when it wants to request
   * cancellation of a workflow instance. It will result in a new 'WorkflowExecutionCancelRequested'
   * event being written to the workflow history and a new DecisionTask created for the workflow
   * instance so new decisions could be made. It fails with 'EntityNotExistsError' if the workflow
   * is not valid anymore due to completion or doesn't exist.
   *
   * @param cancelRequest
   */
  public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws BadRequestError, EntityNotExistsError, CancellationAlreadyRequestedError,
          ServiceBusyError, DomainNotActiveError, LimitExceededError,
          ClientVersionNotSupportedError, WorkflowExecutionAlreadyCompletedError,
          ServiceClientError;

  /**
   * SignalWorkflowExecution is used to send a signal event to running workflow execution. This
   * results in WorkflowExecutionSignaled event recorded in the history and a decision task being
   * created for the execution.
   *
   * @param signalRequest
   */
  public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError;

  /**
   * SignalWithStartWorkflowExecution is used to ensure sending signal to a workflow. If the
   * workflow is running, this results in WorkflowExecutionSignaled event being recorded in the
   * history and a decision task being created for the execution. If the workflow is not running or
   * not found, this results in WorkflowExecutionStarted and WorkflowExecutionSignaled events being
   * recorded in history, and a decision task being created for the execution
   *
   * @param signalWithStartRequest
   */
  public StartWorkflowExecutionResponse SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, WorkflowExecutionAlreadyStartedError, ClientVersionNotSupportedError,
          ServiceClientError;

  /**
   * SignalWithStartWorkflowExecutionAsync is used to ensure sending signal to a workflow
   * asynchronously. It will push a SignalWithStartWorkflowExecutionRequest to a queue and
   * immediately return a response. The request will be processed by a separate consumer eventually.
   *
   * @param signalWithStartRequest
   */
  public SignalWithStartWorkflowExecutionAsyncResponse SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest)
      throws BadRequestError, WorkflowExecutionAlreadyStartedError, ServiceBusyError,
          DomainNotActiveError, LimitExceededError, EntityNotExistsError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ResetWorkflowExecution reset an existing workflow execution to DecisionTaskCompleted
   * event(exclusive). And it will immediately terminating the current execution instance.
   *
   * @param resetRequest
   */
  public ResetWorkflowExecutionResponse ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError, ServiceClientError;

  /**
   * TerminateWorkflowExecution terminates an existing workflow execution by recording
   * WorkflowExecutionTerminated event in the history and immediately terminating the execution
   * instance.
   *
   * @param terminateRequest
   */
  public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, DomainNotActiveError,
          LimitExceededError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError;

  /**
   * ListOpenWorkflowExecutions is a visibility API to list the open executions in a specific
   * domain.
   *
   * @param listRequest
   */
  public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError, LimitExceededError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ListClosedWorkflowExecutions is a visibility API to list the closed executions in a specific
   * domain.
   *
   * @param listRequest
   */
  public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ListWorkflowExecutions is a visibility API to list workflow executions in a specific domain.
   *
   * @param listRequest
   */
  public ListWorkflowExecutionsResponse ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ListArchivedWorkflowExecutions is a visibility API to list archived workflow executions in a
   * specific domain.
   *
   * @param listRequest
   */
  public ListArchivedWorkflowExecutionsResponse ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ScanWorkflowExecutions is a visibility API to list large amount of workflow executions in a
   * specific domain without order.
   *
   * @param listRequest
   */
  public ListWorkflowExecutionsResponse ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * CountWorkflowExecutions is a visibility API to count of workflow executions in a specific
   * domain.
   *
   * @param countRequest
   */
  public CountWorkflowExecutionsResponse CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest)
      throws BadRequestError, EntityNotExistsError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * GetSearchAttributes is a visibility API to get all legal keys that could be used in list APIs
   */
  public GetSearchAttributesResponse GetSearchAttributes()
      throws ServiceBusyError, ClientVersionNotSupportedError, ServiceClientError;

  /**
   * RespondQueryTaskCompleted is called by application worker to complete a QueryTask (which is a
   * DecisionTask for query) as a result of 'PollForDecisionTask' API call. Completing a QueryTask
   * will unblock the client call to 'QueryWorkflow' API and return the query result to client as a
   * response to 'QueryWorkflow' API call.
   *
   * @param completeRequest
   */
  public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError, ServiceClientError;

  /**
   * Reset the sticky tasklist related information in mutable state of a given workflow. Things
   * cleared are: 1. StickyTaskList 2. StickyScheduleToStartTimeout 3. ClientLibraryVersion 4.
   * ClientFeatureVersion 5. ClientImpl
   *
   * @param resetRequest
   */
  public ResetStickyTaskListResponse ResetStickyTaskList(ResetStickyTaskListRequest resetRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          DomainNotActiveError, ClientVersionNotSupportedError,
          WorkflowExecutionAlreadyCompletedError, ServiceClientError;

  /**
   * QueryWorkflow returns query result for a specified workflow execution
   *
   * @param queryRequest
   */
  public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest)
      throws BadRequestError, EntityNotExistsError, QueryFailedError, LimitExceededError,
          ServiceBusyError, ClientVersionNotSupportedError, ServiceClientError;

  /**
   * DescribeWorkflowExecution returns information about the specified workflow execution.
   *
   * @param describeRequest
   */
  public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * DescribeTaskList returns information about the target tasklist, right now this API returns the
   * pollers which polled this tasklist in last few minutes.
   *
   * @param request
   */
  public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /** GetClusterInfo returns information about cadence cluster */
  public ClusterInfo GetClusterInfo()
      throws InternalServiceError, ServiceBusyError, ServiceClientError;

  /**
   * GetTaskListsByDomain returns the list of all the task lists for a domainName.
   *
   * @param request
   */
  public GetTaskListsByDomainResponse GetTaskListsByDomain(GetTaskListsByDomainRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ClientVersionNotSupportedError, ServiceClientError;

  /**
   * ReapplyEvents applies stale events to the current workflow and current run
   *
   * @param request
   */
  public ListTaskListPartitionsResponse ListTaskListPartitions(
      ListTaskListPartitionsRequest request)
      throws BadRequestError, EntityNotExistsError, LimitExceededError, ServiceBusyError,
          ServiceClientError;

  /**
   * RefreshWorkflowTasks refreshes all tasks of a workflow
   *
   * @param request
   */
  public void RefreshWorkflowTasks(RefreshWorkflowTasksRequest request)
      throws BadRequestError, DomainNotActiveError, ServiceBusyError, EntityNotExistsError,
          ServiceClientError;
}

interface AsyncIface {

  public void RegisterDomain(
      RegisterDomainRequest registerRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void DescribeDomain(
      DescribeDomainRequest describeRequest,
      AsyncMethodCallback<DescribeDomainResponse> resultHandler)
      throws ServiceClientError;

  public void ListDomains(
      ListDomainsRequest listRequest, AsyncMethodCallback<ListDomainsResponse> resultHandler)
      throws ServiceClientError;

  public void UpdateDomain(
      UpdateDomainRequest updateRequest, AsyncMethodCallback<UpdateDomainResponse> resultHandler)
      throws ServiceClientError;

  public void DeprecateDomain(
      DeprecateDomainRequest deprecateRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RestartWorkflowExecution(
      RestartWorkflowExecutionRequest restartRequest,
      AsyncMethodCallback<RestartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError;

  public void StartWorkflowExecution(
      StartWorkflowExecutionRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError;

  public void StartWorkflowExecutionAsync(
      StartWorkflowExecutionAsyncRequest startRequest,
      AsyncMethodCallback<StartWorkflowExecutionAsyncResponse> resultHandler)
      throws ServiceClientError;

  public void GetWorkflowExecutionHistory(
      GetWorkflowExecutionHistoryRequest getRequest,
      AsyncMethodCallback<GetWorkflowExecutionHistoryResponse> resultHandler)
      throws ServiceClientError;

  public void PollForDecisionTask(
      PollForDecisionTaskRequest pollRequest,
      AsyncMethodCallback<PollForDecisionTaskResponse> resultHandler)
      throws ServiceClientError;

  public void RespondDecisionTaskCompleted(
      RespondDecisionTaskCompletedRequest completeRequest,
      AsyncMethodCallback<RespondDecisionTaskCompletedResponse> resultHandler)
      throws ServiceClientError;

  public void RespondDecisionTaskFailed(
      RespondDecisionTaskFailedRequest failedRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void PollForActivityTask(
      PollForActivityTaskRequest pollRequest,
      AsyncMethodCallback<PollForActivityTaskResponse> resultHandler)
      throws ServiceClientError;

  public void RecordActivityTaskHeartbeat(
      RecordActivityTaskHeartbeatRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws ServiceClientError;

  public void RecordActivityTaskHeartbeatByID(
      RecordActivityTaskHeartbeatByIDRequest heartbeatRequest,
      AsyncMethodCallback<RecordActivityTaskHeartbeatResponse> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskCompleted(
      RespondActivityTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskCompletedByID(
      RespondActivityTaskCompletedByIDRequest completeRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskFailed(
      RespondActivityTaskFailedRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskFailedByID(
      RespondActivityTaskFailedByIDRequest failRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskCanceled(
      RespondActivityTaskCanceledRequest canceledRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RespondActivityTaskCanceledByID(
      RespondActivityTaskCanceledByIDRequest canceledRequest,
      AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void RequestCancelWorkflowExecution(
      RequestCancelWorkflowExecutionRequest cancelRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void SignalWorkflowExecution(
      SignalWorkflowExecutionRequest signalRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void SignalWithStartWorkflowExecution(
      SignalWithStartWorkflowExecutionRequest signalWithStartRequest,
      AsyncMethodCallback<StartWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError;

  public void SignalWithStartWorkflowExecutionAsync(
      SignalWithStartWorkflowExecutionAsyncRequest signalWithStartRequest,
      AsyncMethodCallback<SignalWithStartWorkflowExecutionAsyncResponse> resultHandler)
      throws ServiceClientError;

  public void ResetWorkflowExecution(
      ResetWorkflowExecutionRequest resetRequest,
      AsyncMethodCallback<ResetWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError;

  public void TerminateWorkflowExecution(
      TerminateWorkflowExecutionRequest terminateRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void ListOpenWorkflowExecutions(
      ListOpenWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListOpenWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void ListClosedWorkflowExecutions(
      ListClosedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListClosedWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void ListWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void ListArchivedWorkflowExecutions(
      ListArchivedWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListArchivedWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void ScanWorkflowExecutions(
      ListWorkflowExecutionsRequest listRequest,
      AsyncMethodCallback<ListWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void CountWorkflowExecutions(
      CountWorkflowExecutionsRequest countRequest,
      AsyncMethodCallback<CountWorkflowExecutionsResponse> resultHandler)
      throws ServiceClientError;

  public void GetSearchAttributes(AsyncMethodCallback<GetSearchAttributesResponse> resultHandler)
      throws ServiceClientError;

  public void RespondQueryTaskCompleted(
      RespondQueryTaskCompletedRequest completeRequest, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;

  public void ResetStickyTaskList(
      ResetStickyTaskListRequest resetRequest,
      AsyncMethodCallback<ResetStickyTaskListResponse> resultHandler)
      throws ServiceClientError;

  public void QueryWorkflow(
      QueryWorkflowRequest queryRequest, AsyncMethodCallback<QueryWorkflowResponse> resultHandler)
      throws ServiceClientError;

  public void DescribeWorkflowExecution(
      DescribeWorkflowExecutionRequest describeRequest,
      AsyncMethodCallback<DescribeWorkflowExecutionResponse> resultHandler)
      throws ServiceClientError;

  public void DescribeTaskList(
      DescribeTaskListRequest request, AsyncMethodCallback<DescribeTaskListResponse> resultHandler)
      throws ServiceClientError;

  public void GetClusterInfo(AsyncMethodCallback<ClusterInfo> resultHandler)
      throws ServiceClientError;

  public void GetTaskListsByDomain(
      GetTaskListsByDomainRequest request,
      AsyncMethodCallback<GetTaskListsByDomainResponse> resultHandler)
      throws ServiceClientError;

  public void ListTaskListPartitions(
      ListTaskListPartitionsRequest request,
      AsyncMethodCallback<ListTaskListPartitionsResponse> resultHandler)
      throws ServiceClientError;

  public void RefreshWorkflowTasks(
      RefreshWorkflowTasksRequest request, AsyncMethodCallback<Void> resultHandler)
      throws ServiceClientError;
}
