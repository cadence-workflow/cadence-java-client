package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ResetWorkflowExecutionRequest {
    private String domain;
    private WorkflowExecution workflowExecution;
    private String reason;
    private Long decisionFinishEventId;
    private String requestId;
    private boolean skipSignalReapply;
}
