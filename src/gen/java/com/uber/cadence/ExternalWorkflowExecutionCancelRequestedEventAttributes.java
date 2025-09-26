package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExternalWorkflowExecutionCancelRequestedEventAttributes {
  private long initiatedEventId;
  private String domain;
  private WorkflowExecution workflowExecution;
}
