package com.uber.cadence.entities;


import lombok.Data;

@Data
public class WorkflowQueryResult {
    private QueryResultType resultType;
    private byte[] answer;
    private String errorMessage;
}
