package com.uber.cadence.entities;

import lombok.Data;

@Data
public class GetWorkflowExecutionHistoryRequest {
    private String domain;
    private WorkflowExecution execution;
    private int maximumPageSize;
    private boolean waitForNewEvent;
    private HistoryEventFilterType HistoryEventFilterType;
    private boolean skipArchival;
    private byte[] nextPageToken;
}
