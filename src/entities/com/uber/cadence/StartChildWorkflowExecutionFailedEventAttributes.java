package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StartChildWorkflowExecutionFailedEventAttributes {
  private String domain;
  private String workflowId;
  private WorkflowType workflowType;
  private ChildWorkflowExecutionFailedCause cause;
  private byte[] control;
  private long initiatedEventId;
  private long decisionTaskCompletedEventId;
}
