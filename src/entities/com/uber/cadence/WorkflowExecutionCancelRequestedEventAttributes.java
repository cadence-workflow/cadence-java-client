package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionCancelRequestedEventAttributes {
  private String cause;
  private long externalInitiatedEventId;
  private WorkflowExecution externalWorkflowExecution;
  private String identity;
  private String requestId;
}
