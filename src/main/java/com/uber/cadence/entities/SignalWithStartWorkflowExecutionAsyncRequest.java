package com.uber.cadence.entities;

import lombok.Data;

@Data
public class SignalWithStartWorkflowExecutionAsyncRequest {
    private SignalWithStartWorkflowExecutionRequest request;
}
