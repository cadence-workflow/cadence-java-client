package com.uber.cadence.entities;

import lombok.Data;

@Data
public class CountWorkflowExecutionsRequest {
    private String domain;
    private String query;
}
