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
package com.uber.cadence.internal.compatibility.proto.mappers;

import static com.uber.cadence.EventType.*;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.cancelExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.childWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.continueAsNewInitiator;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.decisionTaskFailedCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.decisionTaskTimedOutCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.signalExternalWorkflowExecutionFailedCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.timeoutType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.timeToUnixNano;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.externalInitiatedId;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.externalWorkflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.failureDetails;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.failureReason;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.parentDomainName;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.parentInitiatedId;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.parentWorkflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.resetPoints;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowType;

import java.util.ArrayList;
import java.util.List;

class HistoryMapper {

  static com.uber.cadence.History history(com.uber.cadence.api.v1.History t) {
    if (t == null || t == com.uber.cadence.api.v1.History.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.History history = new com.uber.cadence.History();
    history.setEvents(historyEventArray(t.getEventsList()));
    return history;
  }

  static List<com.uber.cadence.HistoryEvent> historyEventArray(
      List<com.uber.cadence.api.v1.HistoryEvent> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.HistoryEvent> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(historyEvent(t.get(i)));
    }
    return v;
  }

  static com.uber.cadence.HistoryEvent historyEvent(com.uber.cadence.api.v1.HistoryEvent e) {
    if (e == null || e == com.uber.cadence.api.v1.HistoryEvent.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.HistoryEvent event = new com.uber.cadence.HistoryEvent();
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

  static com.uber.cadence.ActivityTaskCancelRequestedEventAttributes
      activityTaskCancelRequestedEventAttributes(
          com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ActivityTaskCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskCancelRequestedEventAttributes res =
        new com.uber.cadence.ActivityTaskCancelRequestedEventAttributes();
    res.setActivityId(t.getActivityId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.ActivityTaskCanceledEventAttributes activityTaskCanceledEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskCanceledEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskCanceledEventAttributes res =
        new com.uber.cadence.ActivityTaskCanceledEventAttributes();
    res.setDetails(payload(t.getDetails()));
    res.setLatestCancelRequestedEventId(t.getLatestCancelRequestedEventId());
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.ActivityTaskCompletedEventAttributes activityTaskCompletedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskCompletedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskCompletedEventAttributes res =
        new com.uber.cadence.ActivityTaskCompletedEventAttributes();
    res.setResult(payload(t.getResult()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.ActivityTaskFailedEventAttributes activityTaskFailedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskFailedEventAttributes res =
        new com.uber.cadence.ActivityTaskFailedEventAttributes();
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.ActivityTaskScheduledEventAttributes activityTaskScheduledEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskScheduledEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskScheduledEventAttributes res =
        new com.uber.cadence.ActivityTaskScheduledEventAttributes();
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

  static com.uber.cadence.ActivityTaskStartedEventAttributes activityTaskStartedEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskStartedEventAttributes res =
        new com.uber.cadence.ActivityTaskStartedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setIdentity(t.getIdentity());
    res.setRequestId(t.getRequestId());
    res.setAttempt(t.getAttempt());
    res.setLastFailureReason(failureReason(t.getLastFailure()));
    res.setLastFailureDetails(failureDetails(t.getLastFailure()));
    return res;
  }

  static com.uber.cadence.ActivityTaskTimedOutEventAttributes activityTaskTimedOutEventAttributes(
      com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ActivityTaskTimedOutEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ActivityTaskTimedOutEventAttributes res =
        new com.uber.cadence.ActivityTaskTimedOutEventAttributes();
    res.setDetails(payload(t.getDetails()));
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setTimeoutType(EnumMapper.timeoutType(t.getTimeoutType()));
    res.setLastFailureReason(failureReason(t.getLastFailure()));
    res.setLastFailureDetails(failureDetails(t.getLastFailure()));
    return res;
  }

  static com.uber.cadence.CancelTimerFailedEventAttributes cancelTimerFailedEventAttributes(
      com.uber.cadence.api.v1.CancelTimerFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.CancelTimerFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.CancelTimerFailedEventAttributes res =
        new com.uber.cadence.CancelTimerFailedEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setCause(t.getCause());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionCanceledEventAttributes
      childWorkflowExecutionCanceledEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionCanceledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionCanceledEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionCanceledEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setDetails(payload(t.getDetails()));
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionCompletedEventAttributes
      childWorkflowExecutionCompletedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionCompletedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionCompletedEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionCompletedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setResult(payload(t.getResult()));
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionFailedEventAttributes
      childWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionFailedEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionFailedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionStartedEventAttributes
      childWorkflowExecutionStartedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionStartedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionStartedEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionStartedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionTerminatedEventAttributes
      childWorkflowExecutionTerminatedEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionTerminatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionTerminatedEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionTerminatedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    return res;
  }

  static com.uber.cadence.ChildWorkflowExecutionTimedOutEventAttributes
      childWorkflowExecutionTimedOutEventAttributes(
          com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ChildWorkflowExecutionTimedOutEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ChildWorkflowExecutionTimedOutEventAttributes res =
        new com.uber.cadence.ChildWorkflowExecutionTimedOutEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setTimeoutType(EnumMapper.timeoutType(t.getTimeoutType()));
    return res;
  }

  static com.uber.cadence.DecisionTaskFailedEventAttributes decisionTaskFailedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskFailedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.DecisionTaskFailedEventAttributes res =
        new com.uber.cadence.DecisionTaskFailedEventAttributes();
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

  static com.uber.cadence.DecisionTaskScheduledEventAttributes decisionTaskScheduledEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskScheduledEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.DecisionTaskScheduledEventAttributes res =
        new com.uber.cadence.DecisionTaskScheduledEventAttributes();
    res.setTaskList(taskList(t.getTaskList()));
    res.setStartToCloseTimeoutSeconds(durationToSeconds(t.getStartToCloseTimeout()));
    res.setAttempt(t.getAttempt());
    return res;
  }

  static com.uber.cadence.DecisionTaskStartedEventAttributes decisionTaskStartedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.DecisionTaskStartedEventAttributes res =
        new com.uber.cadence.DecisionTaskStartedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setIdentity(t.getIdentity());
    res.setRequestId(t.getRequestId());
    return res;
  }

  static com.uber.cadence.DecisionTaskCompletedEventAttributes decisionTaskCompletedEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskCompletedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.DecisionTaskCompletedEventAttributes res =
        new com.uber.cadence.DecisionTaskCompletedEventAttributes();
    res.setScheduledEventId(t.getScheduledEventId());
    res.setStartedEventId(t.getStartedEventId());
    res.setIdentity(t.getIdentity());
    res.setBinaryChecksum(t.getBinaryChecksum());
    res.setExecutionContext(byteStringToArray(t.getExecutionContext()));
    return res;
  }

  static com.uber.cadence.DecisionTaskTimedOutEventAttributes decisionTaskTimedOutEventAttributes(
      com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.DecisionTaskTimedOutEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.DecisionTaskTimedOutEventAttributes res =
        new com.uber.cadence.DecisionTaskTimedOutEventAttributes();
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

  static com.uber.cadence.ExternalWorkflowExecutionCancelRequestedEventAttributes
      externalWorkflowExecutionCancelRequestedEventAttributes(
          com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ExternalWorkflowExecutionCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ExternalWorkflowExecutionCancelRequestedEventAttributes res =
        new com.uber.cadence.ExternalWorkflowExecutionCancelRequestedEventAttributes();
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    return res;
  }

  static com.uber.cadence.ExternalWorkflowExecutionSignaledEventAttributes
      externalWorkflowExecutionSignaledEventAttributes(
          com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.ExternalWorkflowExecutionSignaledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.ExternalWorkflowExecutionSignaledEventAttributes res =
        new com.uber.cadence.ExternalWorkflowExecutionSignaledEventAttributes();
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static com.uber.cadence.MarkerRecordedEventAttributes markerRecordedEventAttributes(
      com.uber.cadence.api.v1.MarkerRecordedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.MarkerRecordedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.MarkerRecordedEventAttributes res =
        new com.uber.cadence.MarkerRecordedEventAttributes();
    res.setMarkerName(t.getMarkerName());
    res.setDetails(payload(t.getDetails()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setHeader(header(t.getHeader()));
    return res;
  }

  static com.uber.cadence.RequestCancelActivityTaskFailedEventAttributes
      requestCancelActivityTaskFailedEventAttributes(
          com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.RequestCancelActivityTaskFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.RequestCancelActivityTaskFailedEventAttributes res =
        new com.uber.cadence.RequestCancelActivityTaskFailedEventAttributes();
    res.setActivityId(t.getActivityId());
    res.setCause(t.getCause());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.RequestCancelExternalWorkflowExecutionFailedEventAttributes
      requestCancelExternalWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.RequestCancelExternalWorkflowExecutionFailedEventAttributes res =
        new com.uber.cadence.RequestCancelExternalWorkflowExecutionFailedEventAttributes();
    res.setCause(cancelExternalWorkflowExecutionFailedCause(t.getCause()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static com.uber.cadence.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
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
    com.uber.cadence.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes res =
        new com.uber.cadence.RequestCancelExternalWorkflowExecutionInitiatedEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setChildWorkflowOnly(t.getChildWorkflowOnly());
    return res;
  }

  static com.uber.cadence.SignalExternalWorkflowExecutionFailedEventAttributes
      signalExternalWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.SignalExternalWorkflowExecutionFailedEventAttributes res =
        new com.uber.cadence.SignalExternalWorkflowExecutionFailedEventAttributes();
    res.setCause(signalExternalWorkflowExecutionFailedCause(t.getCause()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setControl(byteStringToArray(t.getControl()));
    return res;
  }

  static com.uber.cadence.SignalExternalWorkflowExecutionInitiatedEventAttributes
      signalExternalWorkflowExecutionInitiatedEventAttributes(
          com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.SignalExternalWorkflowExecutionInitiatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.SignalExternalWorkflowExecutionInitiatedEventAttributes res =
        new com.uber.cadence.SignalExternalWorkflowExecutionInitiatedEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDomain(t.getDomain());
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setSignalName(t.getSignalName());
    res.setInput(payload(t.getInput()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setChildWorkflowOnly(t.getChildWorkflowOnly());
    return res;
  }

  static com.uber.cadence.StartChildWorkflowExecutionFailedEventAttributes
      startChildWorkflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.StartChildWorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.StartChildWorkflowExecutionFailedEventAttributes res =
        new com.uber.cadence.StartChildWorkflowExecutionFailedEventAttributes();
    res.setDomain(t.getDomain());
    res.setWorkflowId(t.getWorkflowId());
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setCause(childWorkflowExecutionFailedCause(t.getCause()));
    res.setControl(byteStringToArray(t.getControl()));
    res.setInitiatedEventId(t.getInitiatedEventId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.StartChildWorkflowExecutionInitiatedEventAttributes
      startChildWorkflowExecutionInitiatedEventAttributes(
          com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.StartChildWorkflowExecutionInitiatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.StartChildWorkflowExecutionInitiatedEventAttributes res =
        new com.uber.cadence.StartChildWorkflowExecutionInitiatedEventAttributes();
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
    return res;
  }

  static com.uber.cadence.TimerCanceledEventAttributes timerCanceledEventAttributes(
      com.uber.cadence.api.v1.TimerCanceledEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.TimerCanceledEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.TimerCanceledEventAttributes res =
        new com.uber.cadence.TimerCanceledEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartedEventId(t.getStartedEventId());
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.TimerFiredEventAttributes timerFiredEventAttributes(
      com.uber.cadence.api.v1.TimerFiredEventAttributes t) {
    if (t == null || t == com.uber.cadence.api.v1.TimerFiredEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.TimerFiredEventAttributes res =
        new com.uber.cadence.TimerFiredEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartedEventId(t.getStartedEventId());
    return res;
  }

  static com.uber.cadence.TimerStartedEventAttributes timerStartedEventAttributes(
      com.uber.cadence.api.v1.TimerStartedEventAttributes t) {
    if (t == null
        || t == com.uber.cadence.api.v1.TimerStartedEventAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.TimerStartedEventAttributes res =
        new com.uber.cadence.TimerStartedEventAttributes();
    res.setTimerId(t.getTimerId());
    res.setStartToFireTimeoutSeconds(durationToSeconds(t.getStartToFireTimeout()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.UpsertWorkflowSearchAttributesEventAttributes
      upsertWorkflowSearchAttributesEventAttributes(
          com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.UpsertWorkflowSearchAttributesEventAttributes res =
        new com.uber.cadence.UpsertWorkflowSearchAttributesEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    return res;
  }

  static com.uber.cadence.WorkflowExecutionCancelRequestedEventAttributes
      workflowExecutionCancelRequestedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCancelRequestedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionCancelRequestedEventAttributes res =
        new com.uber.cadence.WorkflowExecutionCancelRequestedEventAttributes();
    res.setCause(t.getCause());
    res.setExternalInitiatedEventId(externalInitiatedId(t.getExternalExecutionInfo()));
    res.setExternalWorkflowExecution(externalWorkflowExecution(t.getExternalExecutionInfo()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.WorkflowExecutionCanceledEventAttributes
      workflowExecutionCanceledEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCanceledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionCanceledEventAttributes res =
        new com.uber.cadence.WorkflowExecutionCanceledEventAttributes();
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    res.setDetails(payload(t.getDetails()));
    return res;
  }

  static com.uber.cadence.WorkflowExecutionCompletedEventAttributes
      workflowExecutionCompletedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionCompletedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionCompletedEventAttributes res =
        new com.uber.cadence.WorkflowExecutionCompletedEventAttributes();
    res.setResult(payload(t.getResult()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.WorkflowExecutionContinuedAsNewEventAttributes
      workflowExecutionContinuedAsNewEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionContinuedAsNewEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionContinuedAsNewEventAttributes res =
        new com.uber.cadence.WorkflowExecutionContinuedAsNewEventAttributes();
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

  static com.uber.cadence.WorkflowExecutionFailedEventAttributes
      workflowExecutionFailedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionFailedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionFailedEventAttributes res =
        new com.uber.cadence.WorkflowExecutionFailedEventAttributes();
    res.setReason(failureReason(t.getFailure()));
    res.setDetails(failureDetails(t.getFailure()));
    res.setDecisionTaskCompletedEventId(t.getDecisionTaskCompletedEventId());
    return res;
  }

  static com.uber.cadence.WorkflowExecutionSignaledEventAttributes
      workflowExecutionSignaledEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionSignaledEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionSignaledEventAttributes res =
        new com.uber.cadence.WorkflowExecutionSignaledEventAttributes();
    res.setSignalName(t.getSignalName());
    res.setInput(payload(t.getInput()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.WorkflowExecutionStartedEventAttributes
      workflowExecutionStartedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionStartedEventAttributes res =
        new com.uber.cadence.WorkflowExecutionStartedEventAttributes();
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

  static com.uber.cadence.WorkflowExecutionTerminatedEventAttributes
      workflowExecutionTerminatedEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionTerminatedEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionTerminatedEventAttributes res =
        new com.uber.cadence.WorkflowExecutionTerminatedEventAttributes();
    res.setReason(t.getReason());
    res.setDetails(payload(t.getDetails()));
    res.setIdentity(t.getIdentity());
    return res;
  }

  static com.uber.cadence.WorkflowExecutionTimedOutEventAttributes
      workflowExecutionTimedOutEventAttributes(
          com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes t) {
    if (t == null
        || t
            == com.uber.cadence.api.v1.WorkflowExecutionTimedOutEventAttributes
                .getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.WorkflowExecutionTimedOutEventAttributes res =
        new com.uber.cadence.WorkflowExecutionTimedOutEventAttributes();
    res.setTimeoutType(timeoutType(t.getTimeoutType()));
    return res;
  }
}
