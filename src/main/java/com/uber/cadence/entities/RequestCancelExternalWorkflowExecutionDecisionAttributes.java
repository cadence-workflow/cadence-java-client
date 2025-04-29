package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RequestCancelExternalWorkflowExecutionDecisionAttributes {
    private String domain;
    private String workflowId;
    private String runId;
    private boolean childWorkflowOnly;
    private byte[] control;
}
