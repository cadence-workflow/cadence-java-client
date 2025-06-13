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

import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.continueAsNewInitiator;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.longToInt;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.failure;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowRunPair;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowType;

import com.uber.cadence.api.v1.CancelTimerDecisionAttributes;
import com.uber.cadence.api.v1.CancelWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.CompleteWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.ContinueAsNewWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.Decision;
import com.uber.cadence.api.v1.Decision.Builder;
import com.uber.cadence.api.v1.FailWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.RecordMarkerDecisionAttributes;
import com.uber.cadence.api.v1.RequestCancelActivityTaskDecisionAttributes;
import com.uber.cadence.api.v1.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.api.v1.SignalExternalWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.StartChildWorkflowExecutionDecisionAttributes;
import com.uber.cadence.api.v1.StartTimerDecisionAttributes;
import com.uber.cadence.api.v1.UpsertWorkflowSearchAttributesDecisionAttributes;
import java.util.ArrayList;
import java.util.List;

class DecisionMapper {
  static List<Decision> decisionArray(List<com.uber.cadence.entities.Decision> t) {
    if (t == null) {
      return null;
    }

    List<Decision> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(decision(t.get(i)));
    }
    return v;
  }

  static Decision decision(com.uber.cadence.entities.Decision d) {
    if (d == null) {
      return null;
    }
    Builder decision = Decision.newBuilder();
    switch (d.getDecisionType()) {
      case ScheduleActivityTask:
        {
          com.uber.cadence.entities.ScheduleActivityTaskDecisionAttributes attr =
              d.getScheduleActivityTaskDecisionAttributes();
          ScheduleActivityTaskDecisionAttributes.Builder builder =
              ScheduleActivityTaskDecisionAttributes.newBuilder()
                  .setActivityId(attr.getActivityId())
                  .setActivityType(activityType(attr.getActivityType()))
                  .setTaskList(taskList(attr.getTaskList()))
                  .setInput(payload(attr.getInput()))
                  .setScheduleToCloseTimeout(
                      secondsToDuration(attr.getScheduleToCloseTimeoutSeconds()))
                  .setScheduleToStartTimeout(
                      secondsToDuration(attr.getScheduleToStartTimeoutSeconds()))
                  .setStartToCloseTimeout(secondsToDuration(attr.getStartToCloseTimeoutSeconds()))
                  .setHeartbeatTimeout(secondsToDuration(attr.getHeartbeatTimeoutSeconds()))
                  .setHeader(header(attr.getHeader()))
                  .setRequestLocalDispatch(attr.isRequestLocalDispatch());
          if (attr.getRetryPolicy() != null) {
            builder.setRetryPolicy(retryPolicy(attr.getRetryPolicy()));
          }
          if (attr.getDomain() != null) {
            builder.setDomain(attr.getDomain());
          }
          decision.setScheduleActivityTaskDecisionAttributes(builder);
        }
        break;
      case RequestCancelActivityTask:
        {
          com.uber.cadence.entities.RequestCancelActivityTaskDecisionAttributes attr =
              d.getRequestCancelActivityTaskDecisionAttributes();
          decision.setRequestCancelActivityTaskDecisionAttributes(
              RequestCancelActivityTaskDecisionAttributes.newBuilder()
                  .setActivityId(attr.getActivityId()));
        }
        break;
      case StartTimer:
        {
          com.uber.cadence.entities.StartTimerDecisionAttributes attr =
              d.getStartTimerDecisionAttributes();
          decision.setStartTimerDecisionAttributes(
              StartTimerDecisionAttributes.newBuilder()
                  .setTimerId(attr.getTimerId())
                  .setStartToFireTimeout(
                      secondsToDuration(longToInt(attr.getStartToFireTimeoutSeconds()))));
        }
        break;
      case CompleteWorkflowExecution:
        {
          com.uber.cadence.entities.CompleteWorkflowExecutionDecisionAttributes attr =
              d.getCompleteWorkflowExecutionDecisionAttributes();
          decision.setCompleteWorkflowExecutionDecisionAttributes(
              CompleteWorkflowExecutionDecisionAttributes.newBuilder()
                  .setResult(payload(attr.getResult())));
        }
        break;
      case FailWorkflowExecution:
        {
          com.uber.cadence.entities.FailWorkflowExecutionDecisionAttributes attr =
              d.getFailWorkflowExecutionDecisionAttributes();
          decision.setFailWorkflowExecutionDecisionAttributes(
              FailWorkflowExecutionDecisionAttributes.newBuilder()
                  .setFailure(failure(attr.getReason(), attr.getDetails())));
        }
        break;
      case CancelTimer:
        {
          com.uber.cadence.entities.CancelTimerDecisionAttributes attr =
              d.getCancelTimerDecisionAttributes();
          decision.setCancelTimerDecisionAttributes(
              CancelTimerDecisionAttributes.newBuilder().setTimerId(attr.getTimerId()));
        }
        break;
      case CancelWorkflowExecution:
        {
          com.uber.cadence.entities.CancelWorkflowExecutionDecisionAttributes attr =
              d.getCancelWorkflowExecutionDecisionAttributes();
          decision.setCancelWorkflowExecutionDecisionAttributes(
              CancelWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDetails(payload(attr.getDetails())));
        }
        break;
      case RequestCancelExternalWorkflowExecution:
        {
          com.uber.cadence.entities.RequestCancelExternalWorkflowExecutionDecisionAttributes attr =
              d.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
          RequestCancelExternalWorkflowExecutionDecisionAttributes.Builder builder =
              RequestCancelExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain(attr.getDomain())
                  .setWorkflowExecution(workflowRunPair(attr.getWorkflowId(), attr.getRunId()))
                  .setChildWorkflowOnly(attr.isChildWorkflowOnly());
          if (attr.getControl() != null) {
            builder.setControl(arrayToByteString(attr.getControl()));
          }
          decision.setRequestCancelExternalWorkflowExecutionDecisionAttributes(builder);
        }
        break;
      case ContinueAsNewWorkflowExecution:
        {
          com.uber.cadence.entities.ContinueAsNewWorkflowExecutionDecisionAttributes attr =
              d.getContinueAsNewWorkflowExecutionDecisionAttributes();
          ContinueAsNewWorkflowExecutionDecisionAttributes.Builder builder =
              ContinueAsNewWorkflowExecutionDecisionAttributes.newBuilder()
                  .setWorkflowType(workflowType(attr.getWorkflowType()))
                  .setTaskList(taskList(attr.getTaskList()))
                  .setInput(payload(attr.getInput()))
                  .setExecutionStartToCloseTimeout(
                      secondsToDuration(attr.getExecutionStartToCloseTimeoutSeconds()))
                  .setTaskStartToCloseTimeout(
                      secondsToDuration(attr.getTaskStartToCloseTimeoutSeconds()))
                  .setBackoffStartInterval(
                      secondsToDuration(attr.getBackoffStartIntervalInSeconds()))
                  .setInitiator(continueAsNewInitiator(attr.getInitiator()))
                  .setFailure(failure(attr.getFailureReason(), attr.getFailureDetails()))
                  .setLastCompletionResult(payload(attr.getLastCompletionResult()))
                  .setHeader(header(attr.getHeader()))
                  .setMemo(memo(attr.getMemo()))
                  .setSearchAttributes(searchAttributes(attr.getSearchAttributes()));
          if (attr.getRetryPolicy() != null) {
            builder.setRetryPolicy(retryPolicy(attr.getRetryPolicy()));
          }
          if (attr.getCronSchedule() != null) {
            builder.setCronSchedule(attr.getCronSchedule());
          }
          decision.setContinueAsNewWorkflowExecutionDecisionAttributes(builder);
        }
        break;
      case StartChildWorkflowExecution:
        {
          com.uber.cadence.entities.StartChildWorkflowExecutionDecisionAttributes attr =
              d.getStartChildWorkflowExecutionDecisionAttributes();
          StartChildWorkflowExecutionDecisionAttributes.Builder builder =
              StartChildWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain(attr.getDomain())
                  .setWorkflowId(attr.getWorkflowId())
                  .setWorkflowType(workflowType(attr.getWorkflowType()))
                  .setTaskList(taskList(attr.getTaskList()))
                  .setInput(payload(attr.getInput()))
                  .setExecutionStartToCloseTimeout(
                      secondsToDuration(attr.getExecutionStartToCloseTimeoutSeconds()))
                  .setTaskStartToCloseTimeout(
                      secondsToDuration(attr.getTaskStartToCloseTimeoutSeconds()))
                  .setParentClosePolicy(parentClosePolicy(attr.getParentClosePolicy()))
                  .setWorkflowIdReusePolicy(workflowIdReusePolicy(attr.getWorkflowIdReusePolicy()))
                  .setHeader(header(attr.getHeader()))
                  .setMemo(memo(attr.getMemo()))
                  .setSearchAttributes(searchAttributes(attr.getSearchAttributes()));
          if (attr.getRetryPolicy() != null) {
            builder.setRetryPolicy(retryPolicy(attr.getRetryPolicy()));
          }
          if (attr.getControl() != null) {
            builder.setControl(arrayToByteString(attr.getControl()));
          }
          if (attr.getCronSchedule() != null) {
            builder.setCronSchedule(attr.getCronSchedule());
          }
          decision.setStartChildWorkflowExecutionDecisionAttributes(builder);
        }
        break;
      case SignalExternalWorkflowExecution:
        {
          com.uber.cadence.entities.SignalExternalWorkflowExecutionDecisionAttributes attr =
              d.getSignalExternalWorkflowExecutionDecisionAttributes();
          SignalExternalWorkflowExecutionDecisionAttributes.Builder builder =
              SignalExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain(attr.getDomain())
                  .setWorkflowExecution(workflowExecution(attr.getExecution()))
                  .setSignalName(attr.getSignalName())
                  .setInput(payload(attr.getInput()))
                  .setChildWorkflowOnly(attr.isChildWorkflowOnly());
          if (attr.getControl() != null) {
            builder.setControl(arrayToByteString(attr.getControl()));
          }
          decision.setSignalExternalWorkflowExecutionDecisionAttributes(builder);
        }
        break;
      case UpsertWorkflowSearchAttributes:
        {
          com.uber.cadence.entities.UpsertWorkflowSearchAttributesDecisionAttributes attr =
              d.getUpsertWorkflowSearchAttributesDecisionAttributes();
          decision.setUpsertWorkflowSearchAttributesDecisionAttributes(
              UpsertWorkflowSearchAttributesDecisionAttributes.newBuilder()
                  .setSearchAttributes(searchAttributes(attr.getSearchAttributes())));
        }
        break;
      case RecordMarker:
        {
          com.uber.cadence.entities.RecordMarkerDecisionAttributes attr =
              d.getRecordMarkerDecisionAttributes();
          decision.setRecordMarkerDecisionAttributes(
              RecordMarkerDecisionAttributes.newBuilder()
                  .setMarkerName(attr.getMarkerName())
                  .setDetails(payload(attr.getDetails()))
                  .setHeader(header(attr.getHeader())));
        }
        break;
      default:
        throw new IllegalArgumentException("unknown decision type");
    }
    return decision.build();
  }
}
