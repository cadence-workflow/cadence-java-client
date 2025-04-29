package com.uber.cadence.entities;

import lombok.Data;

@Data
public class CompleteWorkflowExecutionDecisionAttributes {
    private byte[] result;
}
