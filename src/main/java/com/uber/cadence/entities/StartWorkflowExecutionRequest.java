package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StartWorkflowExecutionRequest {
    private String domain;
    private String workflowId;
    private WorkflowType workflowType;
    private TaskList taskList;
    private byte[] input;
    private String requestId;
    private int executionStartToCloseTimeoutSeconds;
    private int taskStartToCloseTimeoutSeconds;
    private WorkflowIdReusePolicy workflowIdReusePolicy;
    private Memo memo;
    private SearchAttributes searchAttributes;
    private Header header;
    private int delayStartSeconds;
    private RetryPolicy retryPolicy;
    private String cronSchedule;
    private String identity;
}
