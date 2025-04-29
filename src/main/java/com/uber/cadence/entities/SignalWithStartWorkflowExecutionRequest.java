package com.uber.cadence.entities;

import lombok.Data;

@Data
public class SignalWithStartWorkflowExecutionRequest {
    private String domain;
    private String workflowId;
    private WorkflowType workflowType;
    private TaskList taskList;
    private byte[] input;
    private int executionStartToCloseTimeoutSeconds;
    private int taskStartToCloseTimeoutSeconds;
    private String requestId;
    private Memo memo;
    private SearchAttributes searchAttributes;
    private Header header;
    private RetryPolicy retryPolicy;
    private WorkflowIdReusePolicy workflowIdReusePolicy;
    private String cronSchedule;
    private int delayStartSeconds;
    private String identity;
    private String signalName;
    private byte[] signalInput;
    private byte[] control;
}
