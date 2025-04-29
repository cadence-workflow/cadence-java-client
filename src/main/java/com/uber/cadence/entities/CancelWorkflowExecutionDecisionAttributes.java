package com.uber.cadence.entities;

import lombok.Data;

@Data
public class CancelWorkflowExecutionDecisionAttributes {
    private byte[] details;
}
