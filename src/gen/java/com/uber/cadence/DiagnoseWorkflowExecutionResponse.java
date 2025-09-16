package com.uber.cadence;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiagnoseWorkflowExecutionResponse {
  private String domain;
  private WorkflowExecution diagnosticWorkflowExecution;
}
