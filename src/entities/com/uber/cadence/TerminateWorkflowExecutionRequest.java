package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TerminateWorkflowExecutionRequest {
  private String domain;
  private WorkflowExecution workflowExecution;
  private String reason;
  private byte[] details;
  private String identity;
  private String firstExecutionRunID;
}
