package com.uber.cadence.entities;

import lombok.Data;

@Data
public class WorkflowQuery {
    private String queryType;
    private byte[] queryArgs;
}
