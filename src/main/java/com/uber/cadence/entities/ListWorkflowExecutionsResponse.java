package com.uber.cadence.entities;

import lombok.Data;
import java.util.List;

@Data
public class ListWorkflowExecutionsResponse {
    private List<WorkflowExecutionInfo> executions;
    private byte[] nextPageToken;
}
