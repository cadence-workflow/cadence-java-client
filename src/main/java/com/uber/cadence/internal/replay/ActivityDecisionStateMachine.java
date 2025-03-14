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
import com.uber.cadence.RequestCancelActivityTaskDecisionAttributes;
import com.uber.cadence.ScheduleActivityTaskDecisionAttributes;

final class ActivityDecisionStateMachine extends DecisionStateMachineBase {

  private ScheduleActivityTaskDecisionAttributes scheduleAttributes;

  public ActivityDecisionStateMachine(
      DecisionId id, ScheduleActivityTaskDecisionAttributes scheduleAttributes) {
    super(id);
    this.scheduleAttributes = scheduleAttributes;
  }

  @Override
  public Decision getDecision() {
    switch (state) {
      case CREATED:
        return createScheduleActivityTaskDecision();
      case CANCELED_AFTER_INITIATED:
        return createRequestCancelActivityTaskDecision();
      default:
        return null;
    }
  }

  @Override
  public void handleDecisionTaskStartedEvent() {
    switch (state) {
      case CANCELED_AFTER_INITIATED:
        stateHistory.add("handleDecisionTaskStartedEvent");
        state = DecisionState.CANCELLATION_DECISION_SENT;
        stateHistory.add(state.toString());
        break;
      default:
        super.handleDecisionTaskStartedEvent();
    }
  }

  @Override
  public void handleCancellationFailureEvent(HistoryEvent event) {
    switch (state) {
      case CANCELLATION_DECISION_SENT:
        stateHistory.add("handleCancellationFailureEvent");
        state = DecisionState.INITIATED;
        stateHistory.add(state.toString());
        break;
      default:
        super.handleCancellationFailureEvent(event);
    }
  }

  private Decision createRequestCancelActivityTaskDecision() {
    RequestCancelActivityTaskDecisionAttributes tryCancel =
        new RequestCancelActivityTaskDecisionAttributes();
    tryCancel.setActivityId(scheduleAttributes.getActivityId());
    Decision decision = new Decision();
    decision.setRequestCancelActivityTaskDecisionAttributes(tryCancel);
    decision.setDecisionType(DecisionType.RequestCancelActivityTask);
    return decision;
  }

  private Decision createScheduleActivityTaskDecision() {
    Decision decision = new Decision();
    decision.setScheduleActivityTaskDecisionAttributes(scheduleAttributes);
    decision.setDecisionType(DecisionType.ScheduleActivityTask);
    return decision;
  }
}
