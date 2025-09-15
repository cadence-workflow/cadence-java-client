package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowExecutionStartedEventAttributes {
  private WorkflowType workflowType;
  private String parentWorkflowDomain;
  private WorkflowExecution parentWorkflowExecution;
  private long parentInitiatedEventId;
  private TaskList taskList;
  private byte[] input;
  private int executionStartToCloseTimeoutSeconds;
  private int taskStartToCloseTimeoutSeconds;
  private String continuedExecutionRunId;
  private ContinueAsNewInitiator initiator;
  private String continuedFailureReason;
  private byte[] continuedFailureDetails;
  private byte[] lastCompletionResult;
  private String originalExecutionRunId;
  private String identity;
  private String firstExecutionRunId;
  private long firstScheduledTimeNano;
  private RetryPolicy retryPolicy;
  private int attempt;
  private long expirationTimestamp;
  private String cronSchedule;
  private int firstDecisionTaskBackoffSeconds;
  private Memo memo;
  private SearchAttributes searchAttributes;
  private ResetPoints prevAutoResetPoints;
  private Header header;
  private Map<String, String> partitionConfig;
  private String requestId;
}
