package com.uber.cadence.entities;

import lombok.Data;

@Data
public class SignalWorkflowExecutionRequest {
    private String domain;
    private WorkflowExecution workflowExecution;
    private String signalName;
    private byte[] input;
    private String requestId;
    private byte[] control;
    private String identity;
}
