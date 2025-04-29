package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ResetWorkflowExecutionResponse {
    private String runId;
}
