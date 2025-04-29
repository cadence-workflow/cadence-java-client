package com.uber.cadence.entities;

import lombok.Data;

@Data
public class WorkflowExecutionFilter {
    private String workflowId;
    private String runId;
}
