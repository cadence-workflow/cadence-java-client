/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.compatibility.thrift;

import static com.uber.cadence.EventType.ActivityTaskCancelRequested;
import static com.uber.cadence.EventType.ActivityTaskCanceled;
import static com.uber.cadence.EventType.ActivityTaskCompleted;
import static com.uber.cadence.EventType.ActivityTaskFailed;
import static com.uber.cadence.EventType.ActivityTaskScheduled;
import static com.uber.cadence.EventType.ActivityTaskStarted;
import static com.uber.cadence.EventType.ActivityTaskTimedOut;
import static com.uber.cadence.EventType.CancelTimerFailed;
import static com.uber.cadence.EventType.ChildWorkflowExecutionCanceled;
import static com.uber.cadence.EventType.ChildWorkflowExecutionCompleted;
import static com.uber.cadence.EventType.ChildWorkflowExecutionFailed;
import static com.uber.cadence.EventType.ChildWorkflowExecutionStarted;
import static com.uber.cadence.EventType.ChildWorkflowExecutionTerminated;
import static com.uber.cadence.EventType.ChildWorkflowExecutionTimedOut;
import static com.uber.cadence.EventType.DecisionTaskCompleted;
import static com.uber.cadence.EventType.DecisionTaskFailed;
import static com.uber.cadence.EventType.DecisionTaskScheduled;
import static com.uber.cadence.EventType.DecisionTaskStarted;
import static com.uber.cadence.EventType.DecisionTaskTimedOut;
import static com.uber.cadence.EventType.ExternalWorkflowExecutionCancelRequested;
import static com.uber.cadence.EventType.ExternalWorkflowExecutionSignaled;
import static com.uber.cadence.EventType.MarkerRecorded;
import static com.uber.cadence.EventType.RequestCancelActivityTaskFailed;
import static com.uber.cadence.EventType.RequestCancelExternalWorkflowExecutionFailed;
import static com.uber.cadence.EventType.RequestCancelExternalWorkflowExecutionInitiated;
import static com.uber.cadence.EventType.SignalExternalWorkflowExecutionFailed;
import static com.uber.cadence.EventType.SignalExternalWorkflowExecutionInitiated;
import static com.uber.cadence.EventType.StartChildWorkflowExecutionFailed;
import static com.uber.cadence.EventType.StartChildWorkflowExecutionInitiated;
import static com.uber.cadence.EventType.TimerCanceled;
import static com.uber.cadence.EventType.TimerFired;
import static com.uber.cadence.EventType.TimerStarted;
import static com.uber.cadence.EventType.UpsertWorkflowSearchAttributes;
import static com.uber.cadence.EventType.WorkflowExecutionCancelRequested;
import static com.uber.cadence.EventType.WorkflowExecutionCanceled;
import static com.uber.cadence.EventType.WorkflowExecutionCompleted;
import static com.uber.cadence.EventType.WorkflowExecutionContinuedAsNew;
import static com.uber.cadence.EventType.WorkflowExecutionFailed;
import static com.uber.cadence.EventType.WorkflowExecutionSignaled;
import static com.uber.cadence.EventType.WorkflowExecutionStarted;
import static com.uber.cadence.EventType.WorkflowExecutionTerminated;
import static com.uber.cadence.EventType.WorkflowExecutionTimedOut;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.cancelExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.childWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.continueAsNewInitiator;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.decisionTaskFailedCause;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.decisionTaskTimedOutCause;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.signalExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.timeoutType;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.timeToUnixNano;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.externalInitiatedId;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.externalWorkflowExecution;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.failureDetails;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.failureReason;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.parentDomainName;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.parentInitiatedId;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.parentWorkflowExecution;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.resetPoints;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowType;

import com.uber.cadence.ActivityTaskCancelRequestedEventAttributes;
import com.uber.cadence.ActivityTaskCanceledEventAttributes;
import com.uber.cadence.ActivityTaskCompletedEventAttributes;
import com.uber.cadence.ActivityTaskFailedEventAttributes;
import com.uber.cadence.ActivityTaskScheduledEventAttributes;
import com.uber.cadence.ActivityTaskStartedEventAttributes;
import com.uber.cadence.ActivityTaskTimedOutEventAttributes;
import com.uber.cadence.CancelTimerFailedEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionCanceledEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionStartedEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionTerminatedEventAttributes;
import com.uber.cadence.ChildWorkflowExecutionTimedOutEventAttributes;
import com.uber.cadence.DecisionTaskCompletedEventAttributes;
import com.uber.cadence.DecisionTaskFailedEventAttributes;
import com.uber.cadence.DecisionTaskScheduledEventAttributes;
import com.uber.cadence.DecisionTaskStartedEventAttributes;
import com.uber.cadence.DecisionTaskTimedOutEventAttributes;
import com.uber.cadence.ExternalWorkflowExecutionCancelRequestedEventAttributes;
import com.uber.cadence.ExternalWorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.History;
import com.uber.cadence.HistoryEvent;
import com.uber.cadence.MarkerRecordedEventAttributes;
import com.uber.cadence.RequestCancelActivityTaskFailedEventAttributes;
import com.uber.cadence.RequestCancelExternalWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.SignalExternalWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.SignalExternalWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.StartChildWorkflowExecutionFailedEventAttributes;
import com.uber.cadence.StartChildWorkflowExecutionInitiatedEventAttributes;
import com.uber.cadence.TimerCanceledEventAttributes;
import com.uber.cadence.TimerFiredEventAttributes;
import com.uber.cadence.TimerStartedEventAttributes;
import com.uber.cadence.UpsertWorkflowSearchAttributesEventAttributes;
import com.uber.cadence.WorkflowExecutionCancelRequestedEventAttributes;
import com.uber.cadence.WorkflowExecutionCanceledEventAttributes;
import com.uber.cadence.WorkflowExecutionCompletedEventAttributes;
import com.uber.cadence.WorkflowExecutionContinuedAsNewEventAttributes;
import com.uber.cadence.WorkflowExecutionFailedEventAttributes;
import com.uber.cadence.WorkflowExecutionSignaledEventAttributes;
import com.uber.cadence.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.WorkflowExecutionTerminatedEventAttributes;
import com.uber.cadence.WorkflowExecutionTimedOutEventAttributes;
import java.util.ArrayList;
import java.util.List;

class HistoryMapper {

  static History history(com.uber.cadence.api.v1.History t) {
    if (t == null || t == com.uber.cadence.api.v1.History.getDefaultInstance()) {
      return null;
    }
    History history = new History();
    history.setEvents(historyEventArray(t.getEventsList()));
    return history;
  }

  static List<HistoryEvent> historyEventArray(List<com.uber.cadence.api.v1.HistoryEvent> t) {
    if (t == null) {
      return null;
    }
    List<HistoryEvent> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(historyEvent(t.get(i)));
    }
    return v;
  }

  static HistoryEvent historyEvent(com.uber.cadence.api.v1.HistoryEvent e) {
    if (e == null || e == com.uber.cadence.api.v1.HistoryEvent.getDefaultInstance()) {
      return null;
    }
    HistoryEvent event = new HistoryEvent();
    event.setEventId(e.getEventId());
    event.setTimestamp(timeToUnixNano(e.getEventTime()));
    event.setVersion(e.getVersion());
    event.setTaskId(e.getTaskId());

    if (e.getWorkflowExecutionStartedEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionStarted);
      event.setWorkflowExecutionStartedEventAttributes(
          workflowExecutionStartedEventAttributes(e.getWorkflowExecutionStartedEventAttributes()));
    } else if (e.getWorkflowExecutionCompletedEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionCompleted);
      event.setWorkflowExecutionCompletedEventAttributes(
          workflowExecutionCompletedEventAttributes(
              e.getWorkflowExecutionCompletedEventAttributes()));
    } else if (e.getWorkflowExecutionFailedEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionFailed);
      event.setWorkflowExecutionFailedEventAttributes(
          workflowExecutionFailedEventAttributes(e.getWorkflowExecutionFailedEventAttributes()));
    } else if (e.getWorkflowExecutionTimedOutEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionTimedOut);
      event.setWorkflowExecutionTimedOutEventAttributes(
          workflowExecutionTimedOutEventAttributes(
              e.getWorkflowExecutionTimedOutEventAttributes()));
    } else if (e.getDecisionTaskScheduledEventAttributes()
        != com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes.getDefaultInstance()) {
      event.setEventType(DecisionTaskScheduled);
      event.setDecisionTaskScheduledEventAttributes(
          decisionTaskScheduledEventAttributes(e.getDecisionTaskScheduledEventAttributes()));
    } else if (e.getDecisionTaskStartedEventAttributes()
        != com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes.getDefaultInstance()) {
      event.setEventType(DecisionTaskStarted);
      event.setDecisionTaskStartedEventAttributes(
          decisionTaskStartedEventAttributes(e.getDecisionTaskStartedEventAttributes()));
    } else if (e.getDecisionTaskCompletedEventAttributes()
        != com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes.getDefaultInstance()) {
      event.setEventType(DecisionTaskCompleted);
      event.setDecisionTaskCompletedEventAttributes(
          decisionTaskCompletedEventAttributes(e.getDecisionTaskCompletedEventAttributes()));
    } else if (e.getDecisionTaskTimedOutEventAttributes()
        != com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes.getDefaultInstance()) {
      event.setEventType(DecisionTaskTimedOut);
      event.setDecisionTaskTimedOutEventAttributes(
          decisionTaskTimedOutEventAttributes(e.getDecisionTaskTimedOutEventAttributes()));
    } else if (e.getDecisionTaskFailedEventAttributes()
        != com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes.getDefaultInstance()) {
      event.setEventType(DecisionTaskFailed);
      event.setDecisionTaskFailedEventAttributes(
          decisionTaskFailedEventAttributes(e.getDecisionTaskFailedEventAttributes()));
    } else if (e.getActivityTaskScheduledEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskScheduled);
      event.setActivityTaskScheduledEventAttributes(
          activityTaskScheduledEventAttributes(e.getActivityTaskScheduledEventAttributes()));
    } else if (e.getActivityTaskStartedEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskStarted);
      event.setActivityTaskStartedEventAttributes(
          activityTaskStartedEventAttributes(e.getActivityTaskStartedEventAttributes()));
    } else if (e.getActivityTaskCompletedEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskCompleted);
      event.setActivityTaskCompletedEventAttributes(
          activityTaskCompletedEventAttributes(e.getActivityTaskCompletedEventAttributes()));
    } else if (e.getActivityTaskFailedEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskFailed);
      event.setActivityTaskFailedEventAttributes(
          activityTaskFailedEventAttributes(e.getActivityTaskFailedEventAttributes()));
    } else if (e.getActivityTaskTimedOutEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskTimedOut);
      event.setActivityTaskTimedOutEventAttributes(
          activityTaskTimedOutEventAttributes(e.getActivityTaskTimedOutEventAttributes()));
    } else if (e.getTimerStartedEventAttributes()
        != com.uber.cadence.api.v1.TimerStartedEventAttributes.getDefaultInstance()) {
      event.setEventType(TimerStarted);
      event.setTimerStartedEventAttributes(
          timerStartedEventAttributes(e.getTimerStartedEventAttributes()));
    } else if (e.getTimerFiredEventAttributes()
        != com.uber.cadence.api.v1.TimerFiredEventAttributes.getDefaultInstance()) {
      event.setEventType(TimerFired);
      event.setTimerFiredEventAttributes(
          timerFiredEventAttributes(e.getTimerFiredEventAttributes()));
    } else if (e.getActivityTaskCancelRequestedEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ActivityTaskCancelRequested);
      event.setActivityTaskCancelRequestedEventAttributes(
          activityTaskCancelRequestedEventAttributes(
              e.getActivityTaskCancelRequestedEventAttributes()));
    } else if (e.getRequestCancelActivityTaskFailedEventAttributes()
        != com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(RequestCancelActivityTaskFailed);
      event.setRequestCancelActivityTaskFailedEventAttributes(
          requestCancelActivityTaskFailedEventAttributes(
              e.getRequestCancelActivityTaskFailedEventAttributes()));
    } else if (e.getActivityTaskCanceledEventAttributes()
        != com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes.getDefaultInstance()) {
      event.setEventType(ActivityTaskCanceled);
      event.setActivityTaskCanceledEventAttributes(
          activityTaskCanceledEventAttributes(e.getActivityTaskCanceledEventAttributes()));
    } else if (e.getTimerCanceledEventAttributes()
        != com.uber.cadence.api.v1.TimerCanceledEventAttributes.getDefaultInstance()) {
      event.setEventType(TimerCanceled);
      event.setTimerCanceledEventAttributes(
          timerCanceledEventAttributes(e.getTimerCanceledEventAttributes()));
    } else if (e.getCancelTimerFailedEventAttributes()
        != com.uber.cadence.api.v1.CancelTimerFailedEventAttributes.getDefaultInstance()) {
      event.setEventType(CancelTimerFailed);
      event.setCancelTimerFailedEventAttributes(
          cancelTimerFailedEventAttributes(e.getCancelTimerFailedEventAttributes()));
    } else if (e.getMarkerRecordedEventAttributes()
        != com.uber.cadence.api.v1.MarkerRecordedEventAttributes.getDefaultInstance()) {
      event.setEventType(MarkerRecorded);
      event.setMarkerRecordedEventAttributes(
          markerRecordedEventAttributes(e.getMarkerRecordedEventAttributes()));
    } else if (e.getWorkflowExecutionSignaledEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionSignaled);
      event.setWorkflowExecutionSignaledEventAttributes(
          workflowExecutionSignaledEventAttributes(
              e.getWorkflowExecutionSignaledEventAttributes()));
    } else if (e.getWorkflowExecutionTerminatedEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(WorkflowExecutionTerminated);
      event.setWorkflowExecutionTerminatedEventAttributes(
          workflowExecutionTerminatedEventAttributes(
              e.getWorkflowExecutionTerminatedEventAttributes()));
    } else if (e.getWorkflowExecutionCancelRequestedEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(WorkflowExecutionCancelRequested);
      event.setWorkflowExecutionCancelRequestedEventAttributes(
          workflowExecutionCancelRequestedEventAttributes(
              e.getWorkflowExecutionCancelRequestedEventAttributes()));
    } else if (e.getWorkflowExecutionCanceledEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes.getDefaultInstance()) {
      event.setEventType(WorkflowExecutionCanceled);
      event.setWorkflowExecutionCanceledEventAttributes(
          workflowExecutionCanceledEventAttributes(
              e.getWorkflowExecutionCanceledEventAttributes()));
    } else if (e.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes()
        != com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(RequestCancelExternalWorkflowExecutionInitiated);
      event.setRequestCancelExternalWorkflowExecutionInitiatedEventAttributes(
          requestCancelExternalWorkflowExecutionInitiatedEventAttributes(
              e.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes()));
    } else if (e.getRequestCancelExternalWorkflowExecutionFailedEventAttributes()
        != com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(RequestCancelExternalWorkflowExecutionFailed);
      event.setRequestCancelExternalWorkflowExecutionFailedEventAttributes(
          requestCancelExternalWorkflowExecutionFailedEventAttributes(
              e.getRequestCancelExternalWorkflowExecutionFailedEventAttributes()));
    } else if (e.getExternalWorkflowExecutionCancelRequestedEventAttributes()
        != com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ExternalWorkflowExecutionCancelRequested);
      event.setExternalWorkflowExecutionCancelRequestedEventAttributes(
          externalWorkflowExecutionCancelRequestedEventAttributes(
              e.getExternalWorkflowExecutionCancelRequestedEventAttributes()));
    } else if (e.getWorkflowExecutionContinuedAsNewEventAttributes()
        != com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes
            .getDefaultInstance()) {
      event.setEventType(WorkflowExecutionContinuedAsNew);
      event.setWorkflowExecutionContinuedAsNewEventAttributes(
          workflowExecutionContinuedAsNewEventAttributes(
              e.getWorkflowExecutionContinuedAsNewEventAttributes()));
    } else if (e.getStartChildWorkflowExecutionInitiatedEventAttributes()
        != com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(StartChildWorkflowExecutionInitiated);
      event.setStartChildWorkflowExecutionInitiatedEventAttributes(
          startChildWorkflowExecutionInitiatedEventAttributes(
              e.getStartChildWorkflowExecutionInitiatedEventAttributes()));
    } else if (e.getStartChildWorkflowExecutionFailedEventAttributes()
        != com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(StartChildWorkflowExecutionFailed);
      event.setStartChildWorkflowExecutionFailedEventAttributes(
          startChildWorkflowExecutionFailedEventAttributes(
              e.getStartChildWorkflowExecutionFailedEventAttributes()));
    } else if (e.getChildWorkflowExecutionStartedEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionStarted);
      event.setChildWorkflowExecutionStartedEventAttributes(
          childWorkflowExecutionStartedEventAttributes(
              e.getChildWorkflowExecutionStartedEventAttributes()));
    } else if (e.getChildWorkflowExecutionCompletedEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionCompleted);
      event.setChildWorkflowExecutionCompletedEventAttributes(
          childWorkflowExecutionCompletedEventAttributes(
              e.getChildWorkflowExecutionCompletedEventAttributes()));
    } else if (e.getChildWorkflowExecutionFailedEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionFailed);
      event.setChildWorkflowExecutionFailedEventAttributes(
          childWorkflowExecutionFailedEventAttributes(
              e.getChildWorkflowExecutionFailedEventAttributes()));
    } else if (e.getChildWorkflowExecutionCanceledEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionCanceled);
      event.setChildWorkflowExecutionCanceledEventAttributes(
          childWorkflowExecutionCanceledEventAttributes(
              e.getChildWorkflowExecutionCanceledEventAttributes()));
    } else if (e.getChildWorkflowExecutionTimedOutEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionTimedOut);
      event.setChildWorkflowExecutionTimedOutEventAttributes(
          childWorkflowExecutionTimedOutEventAttributes(
              e.getChildWorkflowExecutionTimedOutEventAttributes()));
    } else if (e.getChildWorkflowExecutionTerminatedEventAttributes()
        != com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ChildWorkflowExecutionTerminated);
      event.setChildWorkflowExecutionTerminatedEventAttributes(
          childWorkflowExecutionTerminatedEventAttributes(
              e.getChildWorkflowExecutionTerminatedEventAttributes()));
    } else if (e.getSignalExternalWorkflowExecutionInitiatedEventAttributes()
        != com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(SignalExternalWorkflowExecutionInitiated);
      event.setSignalExternalWorkflowExecutionInitiatedEventAttributes(
          signalExternalWorkflowExecutionInitiatedEventAttributes(
              e.getSignalExternalWorkflowExecutionInitiatedEventAttributes()));
    } else if (e.getSignalExternalWorkflowExecutionFailedEventAttributes()
        != com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes
            .getDefaultInstance()) {
      event.setEventType(SignalExternalWorkflowExecutionFailed);
      event.setSignalExternalWorkflowExecutionFailedEventAttributes(
          signalExternalWorkflowExecutionFailedEventAttributes(
              e.getSignalExternalWorkflowExecutionFailedEventAttributes()));
    } else if (e.getExternalWorkflowExecutionSignaledEventAttributes()
        != com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes
            .getDefaultInstance()) {
      event.setEventType(ExternalWorkflowExecutionSignaled);
      event.setExternalWorkflowExecutionSignaledEventAttributes(
          externalWorkflowExecutionSignaledEventAttributes(
              e.getExternalWorkflowExecutionSignaledEventAttributes()));
    } else if (e.getUpsertWorkflowSearchAttributesEventAttributes()
        != com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes
            .getDefaultInstance()) {
      event.setEventType(UpsertWorkflowSearchAttributes);
      event.setUpsertWorkflowSearchAttributesEventAttributes(
          upsertWorkflowSearchAttributesEventAttributes(
              e.getUpsertWorkflowSearchAttributesEventAttributes()));
    } else {
      throw new IllegalArgumentException("unknown event type");
    }
    return event;
  }

  static ActivityTaskCancelRequestedEventAttributes activityTaskCancelRequestedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ActivityTaskCancelRequestedEventAttributes res =
        new ActivityTaskCancelRequestedEventAttributes();
    res.setActivityId(t.getActivityId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static ActivityTaskCanceledEventAttributes activityTaskCanceledEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskCanceledEventAttributes res = new ActivityTaskCanceledEventAttributes();
    res.setDetails(payload(t.getDetails()));
    res.setLatestCancelRequestedEventId(t.getLatestCancelRequestedEventId());
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static ActivityTaskCompletedEventAttributes activityTaskCompletedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskCompletedEventAttributes res = new ActivityTaskCompletedEventAttributes();
    res.setResult(payload(t.getResult()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static ActivityTaskFailedEventAttributes activityTaskFailedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskFailedEventAttributes res = new ActivityTaskFailedEventAttributes();
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static ActivityTaskScheduledEventAttributes activityTaskScheduledEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskScheduledEventAttributes res = new ActivityTaskScheduledEventAttributes();
    res.setActivityId(t.getActivityId());
    res.setActivityType(activityType(t.getActivityType()));
    res.setDomain(t.getDomain());
    res.setTaskList(taskList(t.getTaskList()));
    res.setInput(payload(t.getInput()));
    res.setScheduleToCloseTimeoutSeconds(durationToSeconds(t.getScheduleToCloseTimeout()));
    res.setScheduleToStartTimeoutSeconds(durationToSeconds(t.getScheduleToStartTimeout()));
    res.setStartToCloseTimeoutSeconds(durationToSeconds(t.getStartToCloseTimeout()));
    res.setHeartbeatTimeoutSeconds(durationToSeconds(t.getHeartbeatTimeout()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setRetryPolicy(retryPolicy(t.getRetryPolicy()));
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static ActivityTaskStartedEventAttributes activityTaskStartedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskStartedEventAttributes res = new ActivityTaskStartedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setIdentity(t.getIdentity());
    res.setRequestId(t.getRequestId());
    res.setAttempt(t.getAttempt());
    res.setLastFailureReason(failureReason(t.getLastFailure()));
    res.setLastFailureDetails(failureDetails(t.getLastFailure()));
    return res;
  }

  static ActivityTaskTimedOutEventAttributes activityTaskTimedOutEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes.getDefaultInstance()) {
      return null;
    }
    ActivityTaskTimedOutEventAttributes res = new ActivityTaskTimedOutEventAttributes();
    res.setDetails(payload(t.getDetails()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setTimeoutType(EnumMapper.timeoutType(t.getTimeoutType()));
    res.setLastFailureReason(failureReason(t.getLastFailure()));
    res.setLastFailureDetails(failureDetails(t.getLastFailure()));
    return res;
  }

  static CancelTimerFailedEventAttributes cancelTimerFailedEventAttributes(
      com.uber.cadence.api.v1.CancelTimerFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.CancelTimerFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    CancelTimerFailedEventAttributes res = new CancelTimerFailedEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setCause(t.getCause());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static ChildWorkflowExecutionCanceledEventAttributes
      childWorkflowExecutionCanceledEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionCanceledEventAttributes res =
        new ChildWorkflowExecutionCanceledEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setDetails(payload(t.getDetails()));
    return res;
  }

  static ChildWorkflowExecutionCompletedEventAttributes
      childWorkflowExecutionCompletedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionCompletedEventAttributes res =
        new ChildWorkflowExecutionCompletedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setResult(payload(t.getResult()));
    return res;
  }

  static ChildWorkflowExecutionFailedEventAttributes childWorkflowExecutionFailedEventAttributes(
      com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionFailedEventAttributes res =
        new ChildWorkflowExecutionFailedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    return res;
  }

  static ChildWorkflowExecutionStartedEventAttributes childWorkflowExecutionStartedEventAttributes(
      com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionStartedEventAttributes res =
        new ChildWorkflowExecutionStartedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static ChildWorkflowExecutionTerminatedEventAttributes
      childWorkflowExecutionTerminatedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionTerminatedEventAttributes res =
        new ChildWorkflowExecutionTerminatedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    return res;
  }

  static ChildWorkflowExecutionTimedOutEventAttributes
      childWorkflowExecutionTimedOutEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ChildWorkflowExecutionTimedOutEventAttributes res =
        new ChildWorkflowExecutionTimedOutEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setTimeoutType(EnumMapper.timeoutType(t.getTimeoutType()));
    return res;
  }

  static DecisionTaskFailedEventAttributes decisionTaskFailedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    DecisionTaskFailedEventAttributes res = new DecisionTaskFailedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setCause(decisionTaskFailedCause(t.getCause()));
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    res.setIdentity(t.getIdentity());
    res.setBaseRunId(t.getBaseRunId());
    res.setNewRunId(t.getNewRunId());
    res.setForkEventVersion(t.getForkEventVersion());
    res.setBinaryChecksum(t.getBinaryChecksum());
    return res;
  }

  static DecisionTaskScheduledEventAttributes decisionTaskScheduledEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes.getDefaultInstance()) {
      return null;
    }
    DecisionTaskScheduledEventAttributes res = new DecisionTaskScheduledEventAttributes();
    res.setTaskList(taskList(t.getTaskList()));
    res.setStartToCloseTimeoutSeconds(durationToSeconds(t.getStartToCloseTimeout()));
    res.setAttempt(t.getAttempt());
    return res;
  }

  static DecisionTaskStartedEventAttributes decisionTaskStartedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    DecisionTaskStartedEventAttributes res = new DecisionTaskStartedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setIdentity(t.getIdentity());
    res.setRequestId(t.getRequestId());
    return res;
  }

  static DecisionTaskCompletedEventAttributes decisionTaskCompletedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes.getDefaultInstance()) {
      return null;
    }
    DecisionTaskCompletedEventAttributes res = new DecisionTaskCompletedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    res.setBinaryChecksum(t.getBinaryChecksum());
    res.setExecutionContext(byteStringToArray(t.getExecutionContext()));
    return res;
  }

  static DecisionTaskTimedOutEventAttributes decisionTaskTimedOutEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes.getDefaultInstance()) {
      return null;
    }
    DecisionTaskTimedOutEventAttributes res = new DecisionTaskTimedOutEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setTimeoutType(timeoutType(t.getTimeoutType()));
    res.setBaseRunId(t.getBaseRunId());
    res.setNewRunId(t.getNewRunId());
    res.setForkEventVersion(t.getForkEventVersion());
    res.setReason(t.getReason());
    res.setCause(decisionTaskTimedOutCause(t.getCause()));
    return res;
  }

  static ExternalWorkflowExecutionCancelRequestedEventAttributes
      externalWorkflowExecutionCancelRequestedEventAttributes(
          com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ExternalWorkflowExecutionCancelRequestedEventAttributes res =
        new ExternalWorkflowExecutionCancelRequestedEventAttributes();
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    return res;
  }

  static ExternalWorkflowExecutionSignaledEventAttributes
      externalWorkflowExecutionSignaledEventAttributes(
          com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    ExternalWorkflowExecutionSignaledEventAttributes res =
        new ExternalWorkflowExecutionSignaledEventAttributes();
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static MarkerRecordedEventAttributes markerRecordedEventAttributes(
      com.uber.cadence.api.v1.MarkerRecordedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.MarkerRecordedEventAttributes.getDefaultInstance()) {
      return null;
    }
    MarkerRecordedEventAttributes res = new MarkerRecordedEventAttributes();
    res.setMarkerName(t.getMarkerName());
    res.setDetails(payload(t.getDetails()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static RequestCancelActivityTaskFailedEventAttributes
      requestCancelActivityTaskFailedEventAttributes(
          com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    RequestCancelActivityTaskFailedEventAttributes res =
        new RequestCancelActivityTaskFailedEventAttributes();
    res.setActivityId(t.getActivityId());
    res.setCause(t.getCause());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static RequestCancelExternalWorkflowExecutionFailedEventAttributes
      requestCancelExternalWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    RequestCancelExternalWorkflowExecutionFailedEventAttributes res =
        new RequestCancelExternalWorkflowExecutionFailedEventAttributes();
    res.setCause(cancelExternalWorkflowExecutionFailedCause(t.getCause()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
      requestCancelExternalWorkflowExecutionInitiatedEventAttributes(
          com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
              t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1
                .RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    RequestCancelExternalWorkflowExecutionInitiatedEventAttributes res =
        new RequestCancelExternalWorkflowExecutionInitiatedEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setChildWorkflowOnly(t.getChildWorkflowOnly());
    return res;
  }

  static SignalExternalWorkflowExecutionFailedEventAttributes
      signalExternalWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    SignalExternalWorkflowExecutionFailedEventAttributes res =
        new SignalExternalWorkflowExecutionFailedEventAttributes();
    res.setCause(signalExternalWorkflowExecutionFailedCause(t.getCause()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static SignalExternalWorkflowExecutionInitiatedEventAttributes
      signalExternalWorkflowExecutionInitiatedEventAttributes(
          com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    SignalExternalWorkflowExecutionInitiatedEventAttributes res =
        new SignalExternalWorkflowExecutionInitiatedEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setSignalName(t.getSignalName());
    res.setInput(payload(t.getInput()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setChildWorkflowOnly(t.getChildWorkflowOnly());
    return res;
  }

  static StartChildWorkflowExecutionFailedEventAttributes
      startChildWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    StartChildWorkflowExecutionFailedEventAttributes res =
        new StartChildWorkflowExecutionFailedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowId(t.getWorkflowId());
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setCause(childWorkflowExecutionFailedCause(t.getCause()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static StartChildWorkflowExecutionInitiatedEventAttributes
      startChildWorkflowExecutionInitiatedEventAttributes(
          com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    StartChildWorkflowExecutionInitiatedEventAttributes res =
        new StartChildWorkflowExecutionInitiatedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowId(t.getWorkflowId());
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setTaskList(taskList(t.getTaskList()));
    res.setInput(payload(t.getInput()));
    res.setExecutionStartToCloseTimeoutSeconds(
        durationToSeconds(t.getExecutionStartToCloseTimeout()));
    res.setTaskStartToCloseTimeoutSeconds(durationToSeconds(t.getTaskStartToCloseTimeout()));
    res.setParentClosePolicy(parentClosePolicy(t.getParentClosePolicy()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()));
    res.setRetryPolicy(retryPolicy(t.getRetryPolicy()));
    res.setCronSchedule(t.getCronSchedule());
    res.setHeader(header(t.getHeader()));
    res.setMemo(memo(t.getMemo()));
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    res.setDelayStartSeconds(durationToSeconds(t.getDelayStart()));
    res.setJitterStartSeconds(durationToSeconds(t.getJitterStart()));
    res.setFirstRunAtTimestamp(timeToUnixNano(t.getFirstRunAt()));
    return res;
  }

  static TimerCanceledEventAttributes timerCanceledEventAttributes(
      com.uber.cadence.api.v1.TimerCanceledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.TimerCanceledEventAttributes.getDefaultInstance()) {
      return null;
    }
    TimerCanceledEventAttributes res = new TimerCanceledEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartedEventId(t.getStartedEventId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static TimerFiredEventAttributes timerFiredEventAttributes(
      com.uber.cadence.api.v1.TimerFiredEventAttributes t) {
    if (t == null || t == com.uber.cadence.api.v1.TimerFiredEventAttributes.getDefaultInstance()) {
      return null;
    }
    TimerFiredEventAttributes res = new TimerFiredEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartedEventId(t.getStartedEventId());
    return res;
  }

  static TimerStartedEventAttributes timerStartedEventAttributes(
      com.uber.cadence.api.v1.TimerStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.TimerStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    TimerStartedEventAttributes res = new TimerStartedEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartToFireTimeoutSeconds(durationToSeconds(t.getStartToFireTimeout()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static UpsertWorkflowSearchAttributesEventAttributes
      upsertWorkflowSearchAttributesEventAttributes(
          com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    UpsertWorkflowSearchAttributesEventAttributes res =
        new UpsertWorkflowSearchAttributesEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    return res;
  }

  static WorkflowExecutionCancelRequestedEventAttributes
      workflowExecutionCancelRequestedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionCancelRequestedEventAttributes res =
        new WorkflowExecutionCancelRequestedEventAttributes();
    res.setCause(t.getCause());
    res.setExternalInitiatedEventId(externalInitiatedId(t.getExternalExecutionInfo()));
    res.setExternalWorkflowExecution(externalWorkflowExecution(t.getExternalExecutionInfo()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static WorkflowExecutionCanceledEventAttributes workflowExecutionCanceledEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionCanceledEventAttributes res = new WorkflowExecutionCanceledEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDetails(payload(t.getDetails()));
    return res;
  }

  static WorkflowExecutionCompletedEventAttributes workflowExecutionCompletedEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionCompletedEventAttributes res = new WorkflowExecutionCompletedEventAttributes();
    res.setResult(payload(t.getResult()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static WorkflowExecutionContinuedAsNewEventAttributes
      workflowExecutionContinuedAsNewEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionContinuedAsNewEventAttributes res =
        new WorkflowExecutionContinuedAsNewEventAttributes();
    res.setNewExecutionRunId(t.getNewExecutionRunId());
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setTaskList(taskList(t.getTaskList()));
    res.setInput(payload(t.getInput()));
    res.setExecutionStartToCloseTimeoutSeconds(
        durationToSeconds(t.getExecutionStartToCloseTimeout()));
    res.setTaskStartToCloseTimeoutSeconds(durationToSeconds(t.getTaskStartToCloseTimeout()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setBackoffStartIntervalInSeconds(durationToSeconds(t.getBackoffStartInterval()));
    res.setInitiator(continueAsNewInitiator(t.getInitiator()));
    res.setFailureReason(failureReason(t.getFailure()));
    res.setFailureDetails(failureDetails(t.getFailure()));
    res.setLastCompletionResult(payload(t.getLastCompletionResult()));
    res.setHeader(header(t.getHeader()));
    res.setMemo(memo(t.getMemo()));
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    return res;
  }

  static WorkflowExecutionFailedEventAttributes workflowExecutionFailedEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionFailedEventAttributes res = new WorkflowExecutionFailedEventAttributes();
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static WorkflowExecutionSignaledEventAttributes workflowExecutionSignaledEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionSignaledEventAttributes res = new WorkflowExecutionSignaledEventAttributes();
    res.setSignalName(t.getSignalName());
    res.setInput(payload(t.getInput()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static WorkflowExecutionStartedEventAttributes workflowExecutionStartedEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionStartedEventAttributes res = new WorkflowExecutionStartedEventAttributes();
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setParentWorkflowDomain(parentDomainName(t.getParentExecutionInfo()));
    res.setParentWorkflowExecution(parentWorkflowExecution(t.getParentExecutionInfo()));
    res.setParentInitiatedEventId(parentInitiatedId(t.getParentExecutionInfo()));
    res.setTaskList(taskList(t.getTaskList()));
    res.setInput(payload(t.getInput()));
    res.setExecutionStartToCloseTimeoutSeconds(
        durationToSeconds(t.getExecutionStartToCloseTimeout()));
    res.setTaskStartToCloseTimeoutSeconds(durationToSeconds(t.getTaskStartToCloseTimeout()));
    res.setContinuedExecutionRunId(t.getContinuedExecutionRunId());
    res.setInitiator(continueAsNewInitiator(t.getInitiator()));
    res.setContinuedFailureReason(failureReason(t.getContinuedFailure()));
    res.setContinuedFailureDetails(failureDetails(t.getContinuedFailure()));
    res.setLastCompletionResult(payload(t.getLastCompletionResult()));
    res.setOriginalExecutionRunId(t.getOriginalExecutionRunId());
    res.setIdentity(t.getIdentity());
    res.setFirstExecutionRunId(t.getFirstExecutionRunId());
    res.setRetryPolicy(retryPolicy(t.getRetryPolicy()));
    res.setAttempt(t.getAttempt());
    res.setExpirationTimestamp(timeToUnixNano(t.getExpirationTime()));
    res.setCronSchedule(t.getCronSchedule());
    res.setFirstDecisionTaskBackoffSeconds(durationToSeconds(t.getFirstDecisionTaskBackoff()));
    res.setMemo(memo(t.getMemo()));
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    res.setPrevAutoResetPoints(resetPoints(t.getPrevAutoResetPoints()));
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static WorkflowExecutionTerminatedEventAttributes workflowExecutionTerminatedEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionTerminatedEventAttributes res =
        new WorkflowExecutionTerminatedEventAttributes();
    res.setReason(t.getReason());
    res.setDetails(payload(t.getDetails()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static WorkflowExecutionTimedOutEventAttributes workflowExecutionTimedOutEventAttributes(
      com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionTimedOutEventAttributes res = new WorkflowExecutionTimedOutEventAttributes();
    res.setTimeoutType(timeoutType(t.getTimeoutType()));
    return res;
  }
}
