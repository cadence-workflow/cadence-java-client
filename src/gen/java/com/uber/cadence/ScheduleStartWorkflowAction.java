package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScheduleStartWorkflowAction {
  private WorkflowType workflowType;
  private TaskList taskList;
  private byte[] input;
  private String workflowIdPrefix;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private RetryPolicy retryPolicy;
  private Memo memo;
  private SearchAttributes searchAttributes;
}
