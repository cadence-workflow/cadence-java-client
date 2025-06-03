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
package com.uber.cadence.internal.compatibility.proto.mappers;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.uber.cadence.entities.Decision;
import com.uber.cadence.entities.DecisionType;
import com.uber.cadence.internal.compatibility.ClientObjects;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class DecisionMapperTest {
  private static final Map<Decision, com.uber.cadence.api.v1.Decision> DECISIONS =
      ImmutableMap.<Decision, com.uber.cadence.api.v1.Decision>builder()
          .put(
              ClientObjects.DECISION_SCHEDULE_ACTIVITY_TASK,
              ProtoObjects.DECISION_SCHEDULE_ACTIVITY_TASK)
          .put(
              ClientObjects.DECISION_REQUEST_CANCEL_ACTIVITY_TASK,
              ProtoObjects.DECISION_REQUEST_CANCEL_ACTIVITY_TASK)
          .put(ClientObjects.DECISION_START_TIMER, ProtoObjects.DECISION_START_TIMER)
          .put(
              ClientObjects.DECISION_COMPLETE_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_COMPLETE_WORKFLOW_EXECUTION)
          .put(
              ClientObjects.DECISION_FAIL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_FAIL_WORKFLOW_EXECUTION)
          .put(ClientObjects.DECISION_CANCEL_TIMER, ProtoObjects.DECISION_CANCEL_TIMER)
          .put(ClientObjects.DECISION_CANCEL_WORKFLOW, ProtoObjects.DECISION_CANCEL_WORKFLOW)
          .put(
              ClientObjects.DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION)
          .put(
              ClientObjects.DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION)
          .put(
              ClientObjects.DECISION_START_CHILD_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_START_CHILD_WORKFLOW_EXECUTION)
          .put(
              ClientObjects.DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION)
          .put(
              ClientObjects.DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES,
              ProtoObjects.DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES)
          .put(ClientObjects.DECISION_RECORD_MARKER, ProtoObjects.DECISION_RECORD_MARKER)
          .build();

  @Test
  public void testMapDecision() {
    for (Map.Entry<Decision, com.uber.cadence.api.v1.Decision> entry : DECISIONS.entrySet()) {
      Assert.assertEquals(
          "Failed to convert decision of type: " + entry.getKey().getDecisionType(),
          entry.getValue(),
          DecisionMapper.decision(entry.getKey()));
    }
  }

  @Test
  public void testAllDecisionTypesCovered() {
    // If IDL changes add a new decision type, this should fail
    Set<DecisionType> expected = EnumSet.allOf(DecisionType.class);
    Set<DecisionType> actual =
        DECISIONS.keySet().stream().map(Decision::getDecisionType).collect(Collectors.toSet());

    Assert.assertEquals(
        "Missing conversion for some DecisionTypes",
        Collections.emptySet(),
        Sets.difference(expected, actual));
  }

  @Test
  public void testAllAttributesSet() {
    // If IDL changes add a new field to decision attributes, this should fail
    for (Map.Entry<Decision, com.uber.cadence.api.v1.Decision> entry : DECISIONS.entrySet()) {
      Decision decision = entry.getKey();
      switch (decision.getDecisionType()) {
        case ScheduleActivityTask:
          assertNoMissingFields(decision.getScheduleActivityTaskDecisionAttributes());
          break;
        case RequestCancelActivityTask:
          assertNoMissingFields(decision.getRequestCancelActivityTaskDecisionAttributes());
          break;
        case StartTimer:
          assertNoMissingFields(decision.getStartTimerDecisionAttributes());
          break;
        case CompleteWorkflowExecution:
          assertNoMissingFields(decision.getCompleteWorkflowExecutionDecisionAttributes());
          break;
        case FailWorkflowExecution:
          assertNoMissingFields(decision.getFailWorkflowExecutionDecisionAttributes());
          break;
        case CancelTimer:
          assertNoMissingFields(decision.getCancelTimerDecisionAttributes());
          break;
        case CancelWorkflowExecution:
          assertNoMissingFields(decision.getCancelWorkflowExecutionDecisionAttributes());
          break;
        case RequestCancelExternalWorkflowExecution:
          assertNoMissingFields(
              decision.getRequestCancelExternalWorkflowExecutionDecisionAttributes());
          break;
        case RecordMarker:
          assertNoMissingFields(decision.getRecordMarkerDecisionAttributes());
          break;
        case ContinueAsNewWorkflowExecution:
          assertNoMissingFields(decision.getContinueAsNewWorkflowExecutionDecisionAttributes());
          break;
        case StartChildWorkflowExecution:
          assertNoMissingFields(decision.getStartChildWorkflowExecutionDecisionAttributes());
          break;
        case SignalExternalWorkflowExecution:
          assertNoMissingFields(decision.getSignalExternalWorkflowExecutionDecisionAttributes());
          break;
        case UpsertWorkflowSearchAttributes:
          assertNoMissingFields(decision.getUpsertWorkflowSearchAttributesDecisionAttributes());
          break;
      }
    }
  }
}
