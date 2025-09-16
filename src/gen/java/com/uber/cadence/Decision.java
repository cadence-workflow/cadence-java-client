package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Decision {
  private DecisionType decisionType;
  private ScheduleActivityTaskDecisionAttributes scheduleActivityTaskDecisionAttributes;
  private StartTimerDecisionAttributes startTimerDecisionAttributes;
  private CompleteWorkflowExecutionDecisionAttributes completeWorkflowExecutionDecisionAttributes;
  private FailWorkflowExecutionDecisionAttributes failWorkflowExecutionDecisionAttributes;
  private RequestCancelActivityTaskDecisionAttributes requestCancelActivityTaskDecisionAttributes;
  private CancelTimerDecisionAttributes cancelTimerDecisionAttributes;
  private CancelWorkflowExecutionDecisionAttributes cancelWorkflowExecutionDecisionAttributes;
  private RequestCancelExternalWorkflowExecutionDecisionAttributes
      requestCancelExternalWorkflowExecutionDecisionAttributes;
  private RecordMarkerDecisionAttributes recordMarkerDecisionAttributes;
  private ContinueAsNewWorkflowExecutionDecisionAttributes
      continueAsNewWorkflowExecutionDecisionAttributes;
  private StartChildWorkflowExecutionDecisionAttributes
      startChildWorkflowExecutionDecisionAttributes;
  private SignalExternalWorkflowExecutionDecisionAttributes
      signalExternalWorkflowExecutionDecisionAttributes;
  private UpsertWorkflowSearchAttributesDecisionAttributes
      upsertWorkflowSearchAttributesDecisionAttributes;
}
