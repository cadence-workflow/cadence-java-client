package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PollForActivityTaskResponse {
    private byte[] taskToken;
    private WorkflowExecution workflowExecution;
    private String activityId;
    private ActivityType activityType;
    private byte[] input;
    private Long scheduledTimestamp;
    private Long startedTimestamp;
    private int scheduleToCloseTimeoutSeconds;
    private int startToCloseTimeoutSeconds;
    private int heartbeatTimeoutSeconds;
    private int attempt;
    private Long scheduledTimestampOfThisAttempt;
    private byte[] heartbeatDetails;
    private WorkflowType workflowType;
    private String workflowDomain;
    private Header header;
}
