package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RestartWorkflowExecutionRequest {
  private String domain;
  private WorkflowExecution workflowExecution;
  private String reason;
  private String identity;
}
