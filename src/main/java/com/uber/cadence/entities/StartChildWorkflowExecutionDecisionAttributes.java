package com.uber.cadence.entities;



import lombok.Data;

@Data
public class StartChildWorkflowExecutionDecisionAttributes {
    private String domain;
    private String workflowId;
    private WorkflowType workflowType;
    private TaskList taskList;
    private byte[] input;
    private Integer executionStartToCloseTimeoutSeconds;
    private Integer taskStartToCloseTimeoutSeconds;
    private ParentClosePolicy parentClosePolicy;
    private WorkflowIdReusePolicy workflowIdReusePolicy;
    private Header header;
    private Memo memo;
    private SearchAttributes searchAttributes;
    private RetryPolicy retryPolicy;
    private byte[] control;
    private String cronSchedule;
}
