package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExternalWorkflowExecutionSignaledEventAttributes {
  private long initiatedEventId;
  private String domain;
  private WorkflowExecution workflowExecution;
  private byte[] control;
}
