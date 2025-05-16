/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.entities;

import lombok.Data;

@Data
public class HistoryEvent {
  private Long eventId;
  private Long timestamp;
  private EventType eventType;
  private Long version;
  private Long taskId;
  private WorkflowExecutionStartedEventAttributes workflowExecutionStartedEventAttributes;
  private WorkflowExecutionCompletedEventAttributes workflowExecutionCompletedEventAttributes;
  private WorkflowExecutionFailedEventAttributes workflowExecutionFailedEventAttributes;
  private WorkflowExecutionTimedOutEventAttributes workflowExecutionTimedOutEventAttributes;
  private DecisionTaskScheduledEventAttributes decisionTaskScheduledEventAttributes;
  private DecisionTaskStartedEventAttributes decisionTaskStartedEventAttributes;
  private DecisionTaskCompletedEventAttributes decisionTaskCompletedEventAttributes;
  private DecisionTaskTimedOutEventAttributes decisionTaskTimedOutEventAttributes;
  private DecisionTaskFailedEventAttributes decisionTaskFailedEventAttributes;
  private ActivityTaskScheduledEventAttributes activityTaskScheduledEventAttributes;
  private ActivityTaskStartedEventAttributes activityTaskStartedEventAttributes;
  private ActivityTaskCompletedEventAttributes activityTaskCompletedEventAttributes;
  private ActivityTaskFailedEventAttributes activityTaskFailedEventAttributes;
  private ActivityTaskTimedOutEventAttributes activityTaskTimedOutEventAttributes;
  private TimerStartedEventAttributes timerStartedEventAttributes;
  private TimerFiredEventAttributes timerFiredEventAttributes;
  private ActivityTaskCancelRequestedEventAttributes activityTaskCancelRequestedEventAttributes;
  private RequestCancelActivityTaskFailedEventAttributes
      requestCancelActivityTaskFailedEventAttributes;
  private ActivityTaskCanceledEventAttributes activityTaskCanceledEventAttributes;
  private TimerCanceledEventAttributes timerCanceledEventAttributes;
  private CancelTimerFailedEventAttributes cancelTimerFailedEventAttributes;
  private MarkerRecordedEventAttributes markerRecordedEventAttributes;
  private WorkflowExecutionSignaledEventAttributes workflowExecutionSignaledEventAttributes;
  private WorkflowExecutionTerminatedEventAttributes workflowExecutionTerminatedEventAttributes;
  private WorkflowExecutionCancelRequestedEventAttributes
      workflowExecutionCancelRequestedEventAttributes;
  private WorkflowExecutionCanceledEventAttributes workflowExecutionCanceledEventAttributes;
  private RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
      requestCancelExternalWorkflowExecutionInitiatedEventAttributes;
  private RequestCancelExternalWorkflowExecutionFailedEventAttributes
      requestCancelExternalWorkflowExecutionFailedEventAttributes;
  private ExternalWorkflowExecutionCancelRequestedEventAttributes
      externalWorkflowExecutionCancelRequestedEventAttributes;
  private WorkflowExecutionContinuedAsNewEventAttributes
      workflowExecutionContinuedAsNewEventAttributes;
  private StartChildWorkflowExecutionInitiatedEventAttributes
      startChildWorkflowExecutionInitiatedEventAttributes;
  private StartChildWorkflowExecutionFailedEventAttributes
      startChildWorkflowExecutionFailedEventAttributes;
  private ChildWorkflowExecutionStartedEventAttributes childWorkflowExecutionStartedEventAttributes;
  private ChildWorkflowExecutionCompletedEventAttributes
      childWorkflowExecutionCompletedEventAttributes;
  private ChildWorkflowExecutionFailedEventAttributes childWorkflowExecutionFailedEventAttributes;
  private ChildWorkflowExecutionCanceledEventAttributes
      childWorkflowExecutionCanceledEventAttributes;
  private ChildWorkflowExecutionTimedOutEventAttributes
      childWorkflowExecutionTimedOutEventAttributes;
  private ChildWorkflowExecutionTerminatedEventAttributes
      childWorkflowExecutionTerminatedEventAttributes;
  private SignalExternalWorkflowExecutionInitiatedEventAttributes
      signalExternalWorkflowExecutionInitiatedEventAttributes;
  private SignalExternalWorkflowExecutionFailedEventAttributes
      signalExternalWorkflowExecutionFailedEventAttributes;
  private ExternalWorkflowExecutionSignaledEventAttributes
      externalWorkflowExecutionSignaledEventAttributes;
  private UpsertWorkflowSearchAttributesEventAttributes
      upsertWorkflowSearchAttributesEventAttributes;
}
