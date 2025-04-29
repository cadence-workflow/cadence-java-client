package com.uber.cadence.entities;


import lombok.Data;

@Data
public class ContinueAsNewWorkflowExecutionDecisionAttributes {
    private WorkflowType workflowType;
    private TaskList taskList;
    private byte[] input;
    private Integer executionStartToCloseTimeoutSeconds;
    private Integer taskStartToCloseTimeoutSeconds;
    private Integer backoffStartIntervalInSeconds;
    private ContinueAsNewInitiator initiator;
    private String failureReason;
    private byte[] failureDetails;
    private byte[] lastCompletionResult;
    private Header header;
    private Memo memo;
    private SearchAttributes searchAttributes;
    private RetryPolicy retryPolicy;
    private String cronSchedule;
}
