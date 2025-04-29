package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class ListOpenWorkflowExecutionsResponse {
    private List<WorkflowExecutionInfo> executions;
    private byte[] nextPageToken;
}
