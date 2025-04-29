package com.uber.cadence.entities;

import lombok.Data;
import java.util.Map;

@Data
public class PollForDecisionTaskResponse {
    private byte[] taskToken;
    private WorkflowExecution workflowExecution;
    private WorkflowType workflowType;
    private Long previousStartedEventId;
    private Long startedEventId;
    private Long attempt;
    private Long backlogCountHint;
    private History history;
    private byte[] nextPageToken;
    private WorkflowQuery query;
    private TaskList workflowExecutionTaskList;
    private Long scheduledTimestamp;
    private Long startedTimestamp;
    private Map<String, WorkflowQuery> queries;
    private Long nextEventId;
}
