package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RequestCancelExternalWorkflowExecutionDecisionAttributes {
  private String domain;
  private String workflowId;
  private String runId;
  private byte[] control;
  private boolean childWorkflowOnly;
}
