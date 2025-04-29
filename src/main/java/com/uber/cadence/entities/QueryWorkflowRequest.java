package com.uber.cadence.entities;

import lombok.Data;

@Data
public class QueryWorkflowRequest {
    private String domain;
    private WorkflowExecution execution;
    private WorkflowQuery query;
    private QueryRejectCondition queryRejectCondition;
    private QueryConsistencyLevel queryConsistencyLevel;
}
