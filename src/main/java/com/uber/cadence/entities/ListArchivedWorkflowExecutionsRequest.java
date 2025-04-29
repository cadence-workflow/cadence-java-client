package com.uber.cadence.entities;

import lombok.Data;

@Data
public class ListArchivedWorkflowExecutionsRequest {
    private String domain;
    private Integer pageSize;
    private byte[] nextPageToken;
    private String query;
}
