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
import com.uber.cadence.serviceclient.exceptions.BadRequestException;
import com.uber.cadence.serviceclient.exceptions.EntityNotExistsException;
import com.uber.cadence.serviceclient.exceptions.InternalServiceException;
import com.uber.cadence.serviceclient.exceptions.WorkflowExecutionAlreadyCompletedException;
import java.util.Optional;
import org.apache.thrift.TException;

interface TestWorkflowMutableState {

  ExecutionId getExecutionId();

  /** @return close status of the workflow or empty if still open */
  Optional<WorkflowExecutionCloseStatus> getCloseStatus();

  StartWorkflowExecutionRequest getStartRequest();

  void startDecisionTask(PollForDecisionTaskResponse task, PollForDecisionTaskRequest pollRequest)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void completeDecisionTask(int historySize, RespondDecisionTaskCompletedRequest request)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void completeSignalExternalWorkflowExecution(String signalId, String runId)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void failSignalExternalWorkflowExecution(
      String signalId, SignalExternalWorkflowExecutionFailedCause cause)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void failDecisionTask(RespondDecisionTaskFailedRequest request)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void childWorkflowStarted(ChildWorkflowExecutionStartedEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void childWorkflowFailed(String workflowId, ChildWorkflowExecutionFailedEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void childWorkflowTimedOut(String activityId, ChildWorkflowExecutionTimedOutEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void failStartChildWorkflow(String workflowId, StartChildWorkflowExecutionFailedEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void childWorkflowCompleted(String workflowId, ChildWorkflowExecutionCompletedEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void childWorkflowCanceled(String workflowId, ChildWorkflowExecutionCanceledEventAttributes a)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void startWorkflow(
      boolean continuedAsNew, Optional<SignalWorkflowExecutionRequest> signalWithStartSignal)
      throws InternalServiceException, BadRequestException;

  void startActivityTask(PollForActivityTaskResponse task, PollForActivityTaskRequest pollRequest)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void completeActivityTask(String activityId, RespondActivityTaskCompletedRequest request)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void completeActivityTaskById(String activityId, RespondActivityTaskCompletedByIDRequest request)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void failActivityTask(String activityId, RespondActivityTaskFailedRequest request)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void failActivityTaskById(String id, RespondActivityTaskFailedByIDRequest failRequest)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  RecordActivityTaskHeartbeatResponse heartbeatActivityTask(String activityId, byte[] details)
      throws InternalServiceException, EntityNotExistsException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void signal(SignalWorkflowExecutionRequest signalRequest)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void signalFromWorkflow(SignalExternalWorkflowExecutionDecisionAttributes a)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void requestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void cancelActivityTask(String id, RespondActivityTaskCanceledRequest canceledRequest)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  void cancelActivityTaskById(String id, RespondActivityTaskCanceledByIDRequest canceledRequest)
      throws EntityNotExistsException, InternalServiceException,
          WorkflowExecutionAlreadyCompletedException, BadRequestException;

  QueryWorkflowResponse query(QueryWorkflowRequest queryRequest) throws TException;

  void completeQuery(QueryId queryId, RespondQueryTaskCompletedRequest completeRequest)
      throws EntityNotExistsException;

  StickyExecutionAttributes getStickyExecutionAttributes();

  Optional<TestWorkflowMutableState> getParent();
}
