package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StartWorkflowExecutionAsyncRequest {
    private StartWorkflowExecutionRequest request;
}
