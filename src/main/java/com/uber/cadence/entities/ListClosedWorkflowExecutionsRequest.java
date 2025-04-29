package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListClosedWorkflowExecutionsRequest {
    private String domain;
    private int maximumPageSize;
    private WorkflowExecutionFilter executionFilter;
    private WorkflowTypeFilter typeFilter;
    private WorkflowExecutionCloseStatus statusFilter;
    private byte[] nextPageToken;
    private StartTimeFilter startTimeFilter;
}
