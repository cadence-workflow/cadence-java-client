package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RequestCancelWorkflowExecutionRequest {
    private String domain;
    private WorkflowExecution workflowExecution;
    private String requestId;
    private String identity;
}
