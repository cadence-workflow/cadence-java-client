package com.uber.cadence.entities;

import lombok.Data;

@Data
public class DescribeWorkflowExecutionRequest {
    private String domain;
    private WorkflowExecution execution;
}
