package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RequestCancelWorkflowExecutionRequest {
  private String domain;
  private WorkflowExecution workflowExecution;
  private String identity;
  private String requestId;
  private String cause;
  private String firstExecutionRunID;
}
