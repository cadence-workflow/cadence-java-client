package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PollForDecisionTaskResponse {
  private byte[] taskToken;
  private WorkflowExecution workflowExecution;
  private WorkflowType workflowType;
  private long previousStartedEventId;
  private long startedEventId;
  private long attempt;
  private long backlogCountHint;
  private History history;
  private byte[] nextPageToken;
  private WorkflowQuery query;
  private TaskList WorkflowExecutionTaskList;
  private long scheduledTimestamp;
  private long startedTimestamp;
  private Map<String, WorkflowQuery> queries;
  private long nextEventId;
  private long totalHistoryBytes;
}
