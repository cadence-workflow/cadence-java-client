package com.uber.cadence;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiagnoseWorkflowExecutionRequest {
  private String domain;
  private WorkflowExecution workflow_execution;
  private String identity;
}
