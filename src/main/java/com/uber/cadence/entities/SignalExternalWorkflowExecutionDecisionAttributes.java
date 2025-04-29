package com.uber.cadence.entities;

import lombok.Data;

@Data
public class SignalExternalWorkflowExecutionDecisionAttributes {
    private String domain;
    private WorkflowExecution execution;
    private String signalName;
    private byte[] input;
    private boolean childWorkflowOnly;
    private byte[] control;
}
