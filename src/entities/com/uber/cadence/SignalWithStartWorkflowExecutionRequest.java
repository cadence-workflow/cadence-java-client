package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignalWithStartWorkflowExecutionRequest {
  private String domain;
  private String workflowId;
  private WorkflowType workflowType;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private String identity;
  private String requestId;
  private WorkflowIdReusePolicy workflowIdReusePolicy;
  private String signalName;
  private byte[] signalInput;
  private byte[] control;
  private RetryPolicy retryPolicy;
  private String cronSchedule;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private Header header;
  private int delayStartSeconds;
  private int jitterStartSeconds;
}
