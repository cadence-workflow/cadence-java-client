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

package com.uber.cadence.internal.replay;

import com.uber.cadence.Decision;
import com.uber.cadence.DecisionType;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.uber.cadence.StartChildWorkflowExecutionDecisionAttributes;

final class ChildWorkflowDecisionStateMachine extends DecisionStateMachineBase {

  private StartChildWorkflowExecutionDecisionAttributes startAttributes;

  public ChildWorkflowDecisionStateMachine(
      DecisionId id, StartChildWorkflowExecutionDecisionAttributes startAttributes) {
    super(id);
    this.startAttributes = startAttributes;
  }

  @Override
  public Decision getDecision() {
    switch (state) {
      case CREATED:
        return createStartChildWorkflowExecutionDecision();
      case CANCELED_AFTER_STARTED:
        return createRequestCancelExternalWorkflowExecutionDecision();
      default:
        return null;
    }
  }

  @Override
  public void handleDecisionTaskStartedEvent() {
    switch (state) {
      case CANCELED_AFTER_STARTED:
        state = DecisionState.CANCELLATION_DECISION_SENT;
        break;
      default:
        super.handleDecisionTaskStartedEvent();
    }
  }

  @Override
  public void handleStartedEvent(HistoryEvent event) {
    stateHistory.add("handleStartedEvent");
    switch (state) {
      case INITIATED:
        state = DecisionState.STARTED;
        break;
      case CANCELED_AFTER_INITIATED:
        state = DecisionState.CANCELED_AFTER_STARTED;
        break;
      default:
    }
    stateHistory.add(state.toString());
  }

  @Override
  public void handleCancellationFailureEvent(HistoryEvent event) {
    switch (state) {
      case CANCELLATION_DECISION_SENT:
        stateHistory.add("handleCancellationFailureEvent");
        state = DecisionState.STARTED;
        stateHistory.add(state.toString());
        break;
      default:
        super.handleCancellationFailureEvent(event);
    }
  }

  @Override
  public boolean cancel(Runnable immediateCancellationCallback) {
    switch (state) {
      case STARTED:
        stateHistory.add("cancel");
        state = DecisionState.CANCELED_AFTER_STARTED;
        stateHistory.add(state.toString());
        return true;
      default:
        return super.cancel(immediateCancellationCallback);
    }
  }

  @Override
  public void handleCancellationEvent() {
    switch (state) {
      case STARTED:
        stateHistory.add("handleCancellationEvent");
        state = DecisionState.COMPLETED;
        stateHistory.add(state.toString());
        break;
      default:
        super.handleCancellationEvent();
    }
  }

  @Override
  public void handleCompletionEvent() {
    switch (state) {
      case STARTED:
      case CANCELED_AFTER_STARTED:
        stateHistory.add("handleCompletionEvent");
        state = DecisionState.COMPLETED;
        stateHistory.add(state.toString());
        break;
      default:
        super.handleCompletionEvent();
    }
  }

  private Decision createRequestCancelExternalWorkflowExecutionDecision() {
    RequestCancelExternalWorkflowExecutionDecisionAttributes tryCancel =
        new RequestCancelExternalWorkflowExecutionDecisionAttributes();
    tryCancel.setWorkflowId(startAttributes.getWorkflowId());
    Decision decision = new Decision();
    decision.setRequestCancelExternalWorkflowExecutionDecisionAttributes(tryCancel);
    decision.setDecisionType(DecisionType.RequestCancelExternalWorkflowExecution);
    return decision;
  }

  private Decision createStartChildWorkflowExecutionDecision() {
    Decision decision = new Decision();
    decision.setStartChildWorkflowExecutionDecisionAttributes(startAttributes);
    decision.setDecisionType(DecisionType.StartChildWorkflowExecution);
    return decision;
  }
}
