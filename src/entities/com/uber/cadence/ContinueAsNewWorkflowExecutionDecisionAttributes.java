package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ContinueAsNewWorkflowExecutionDecisionAttributes {
  private WorkflowType workflowType;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private int backoffStartIntervalInSeconds;
  private RetryPolicy retryPolicy;
  private ContinueAsNewInitiator initiator;
  private String failureReason;
  private byte[] failureDetails;
  private byte[] lastCompletionResult;
  private String cronSchedule;
  private Header header;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private int jitterStartSeconds;
}
