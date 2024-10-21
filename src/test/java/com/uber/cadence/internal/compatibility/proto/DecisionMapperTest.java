package com.uber.cadence.internal.compatibility.proto;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertMissingFields;
import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.uber.cadence.Decision;
import com.uber.cadence.DecisionType;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import com.uber.cadence.internal.compatibility.ThriftObjects;
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
              ThriftObjects.DECISION_SCHEDULE_ACTIVITY_TASK,
              ProtoObjects.DECISION_SCHEDULE_ACTIVITY_TASK)
          .put(
              ThriftObjects.DECISION_REQUEST_CANCEL_ACTIVITY_TASK,
              ProtoObjects.DECISION_REQUEST_CANCEL_ACTIVITY_TASK)
          .put(ThriftObjects.DECISION_START_TIMER, ProtoObjects.DECISION_START_TIMER)
          .put(
              ThriftObjects.DECISION_COMPLETE_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_COMPLETE_WORKFLOW_EXECUTION)
          .put(
              ThriftObjects.DECISION_FAIL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_FAIL_WORKFLOW_EXECUTION)
          .put(ThriftObjects.DECISION_CANCEL_TIMER, ProtoObjects.DECISION_CANCEL_TIMER)
          .put(ThriftObjects.DECISION_CANCEL_WORKFLOW, ProtoObjects.DECISION_CANCEL_WORKFLOW)
          .put(
              ThriftObjects.DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION)
          .put(
              ThriftObjects.DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION)
          .put(
              ThriftObjects.DECISION_START_CHILD_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_START_CHILD_WORKFLOW_EXECUTION)
          .put(
              ThriftObjects.DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION,
              ProtoObjects.DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION)
          .put(
              ThriftObjects.DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES,
              ProtoObjects.DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES)
          .put(ThriftObjects.DECISION_RECORD_MARKER, ProtoObjects.DECISION_RECORD_MARKER)
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
      switch (decision.decisionType) {
        case ScheduleActivityTask:
          assertNoMissingFields(decision.scheduleActivityTaskDecisionAttributes);
          break;
        case RequestCancelActivityTask:
          assertNoMissingFields(decision.requestCancelActivityTaskDecisionAttributes);
          break;
        case StartTimer:
          assertNoMissingFields(decision.startTimerDecisionAttributes);
          break;
        case CompleteWorkflowExecution:
          assertNoMissingFields(decision.completeWorkflowExecutionDecisionAttributes);
          break;
        case FailWorkflowExecution:
          assertNoMissingFields(decision.failWorkflowExecutionDecisionAttributes);
          break;
        case CancelTimer:
          assertNoMissingFields(decision.cancelTimerDecisionAttributes);
          break;
        case CancelWorkflowExecution:
          assertNoMissingFields(decision.cancelWorkflowExecutionDecisionAttributes);
          break;
        case RequestCancelExternalWorkflowExecution:
          assertNoMissingFields(decision.requestCancelExternalWorkflowExecutionDecisionAttributes);
          break;
        case RecordMarker:
          assertNoMissingFields(decision.recordMarkerDecisionAttributes);
          break;
        case ContinueAsNewWorkflowExecution:
          assertMissingFields(
              decision.continueAsNewWorkflowExecutionDecisionAttributes, "jitterStartSeconds");
          break;
        case StartChildWorkflowExecution:
          assertNoMissingFields(decision.startChildWorkflowExecutionDecisionAttributes);
          break;
        case SignalExternalWorkflowExecution:
          assertNoMissingFields(decision.signalExternalWorkflowExecutionDecisionAttributes);
          break;
        case UpsertWorkflowSearchAttributes:
          assertNoMissingFields(decision.upsertWorkflowSearchAttributesDecisionAttributes);
          break;
      }
    }
  }
}
