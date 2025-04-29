package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListOpenWorkflowExecutionsRequest {
    private String domain;
    private int maximumPageSize;
    private WorkflowExecutionFilter executionFilter;
    private WorkflowTypeFilter typeFilter;
    private byte[] nextPageToken;
    private StartTimeFilter startTimeFilter;
}
