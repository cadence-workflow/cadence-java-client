package com.uber.cadence.entities;

import lombok.Data;

@Data
public class TerminateWorkflowExecutionRequest {
    private String domain;
    private WorkflowExecution workflowExecution;
    private String reason;
    private byte[] details;
    private String identity;
}
