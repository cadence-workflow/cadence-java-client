package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignalWorkflowExecutionRequest {
  private String domain;
  private WorkflowExecution workflowExecution;
  private String signalName;
  private byte[] input;
  private String identity;
  private String requestId;
  private byte[] control;
}
