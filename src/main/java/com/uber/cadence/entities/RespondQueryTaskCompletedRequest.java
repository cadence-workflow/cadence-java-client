package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondQueryTaskCompletedRequest {
    private byte[] taskToken;
    private QueryTaskCompletedType completedType;
    private byte[] queryResult;
    private String errorMessage;
    private WorkerVersionInfo workerVersionInfo;
}
