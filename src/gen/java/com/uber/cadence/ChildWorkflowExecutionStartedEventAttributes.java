package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ChildWorkflowExecutionStartedEventAttributes {
  private String domain;
  private long initiatedEventId;
  private WorkflowExecution workflowExecution;
  private WorkflowType workflowType;
  private Header header;
}
