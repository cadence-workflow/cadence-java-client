package com.uber.cadence.entities;

import lombok.Data;

@Data
public class WorkflowExecutionConfiguration {
    private TaskList taskList;
    private int executionStartToCloseTimeoutSeconds;
    private int taskStartToCloseTimeoutSeconds;
    private ChildPolicy childPolicy;
    private WorkflowIdReusePolicy workflowIdReusePolicy;
    private RetryPolicy retryPolicy;
}
