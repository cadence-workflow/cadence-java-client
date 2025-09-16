package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignalExternalWorkflowExecutionDecisionAttributes {
  private String domain;
  private WorkflowExecution execution;
  private String signalName;
  private byte[] input;
  private byte[] control;
  private boolean childWorkflowOnly;
}
