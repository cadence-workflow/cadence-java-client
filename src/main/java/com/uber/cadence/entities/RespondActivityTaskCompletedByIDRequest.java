package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondActivityTaskCompletedByIDRequest {
    private String domain;
    private String workflowID;
    private String runID;
    private String activityID;
    private byte[] result;
    private String identity;
}
