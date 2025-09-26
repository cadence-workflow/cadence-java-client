package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StartChildWorkflowExecutionInitiatedEventAttributes {
  private String domain;
  private String workflowId;
  private WorkflowType workflowType;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private ParentClosePolicy parentClosePolicy;
  private byte[] control;
  private long decisionTaskCompletedEventId;
  private WorkflowIdReusePolicy workflowIdReusePolicy;
  private RetryPolicy retryPolicy;
  private String cronSchedule;
  private Header header;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private int delayStartSeconds;
  private int jitterStartSeconds;
  private long firstRunAtTimestamp;
}
