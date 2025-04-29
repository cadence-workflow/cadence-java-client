package com.uber.cadence.entities;

import lombok.Data;

@Data
public class QueryWorkflowResponse {
    private byte[] queryResult;
    private QueryRejected queryRejected;
}
