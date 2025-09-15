package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionContinuedAsNewEventAttributes {
  private String newExecutionRunId;
  private WorkflowType workflowType;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private long decisionTaskCompletedEventId;
  private int backoffStartIntervalInSeconds;
  private ContinueAsNewInitiator initiator;
  private String failureReason;
  private byte[] failureDetails;
  private byte[] lastCompletionResult;
  private Header header;
  private Memo memo;
  private SearchAttributes searchAttributes;
}
