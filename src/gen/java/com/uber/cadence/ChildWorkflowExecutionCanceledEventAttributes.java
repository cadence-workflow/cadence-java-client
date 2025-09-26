package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ChildWorkflowExecutionCanceledEventAttributes {
  private byte[] details;
  private String domain;
  private WorkflowExecution workflowExecution;
  private WorkflowType workflowType;
  private long initiatedEventId;
  private long startedEventId;
}
