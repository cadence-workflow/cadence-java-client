package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListWorkflowExecutionsRequest {
    private String domain;
    private int pageSize;
    private byte[] nextPageToken;
    private String query;
}
