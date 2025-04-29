package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ResetStickyTaskListRequest {
    private String domain;
    private WorkflowExecution execution;
}
