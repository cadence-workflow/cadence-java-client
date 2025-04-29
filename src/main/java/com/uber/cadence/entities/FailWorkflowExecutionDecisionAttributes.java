package com.uber.cadence.entities;

import lombok.Data;

@Data
public class FailWorkflowExecutionDecisionAttributes {
    private String reason;
    private byte[] details;
}
