package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondActivityTaskFailedByIDRequest {
    private String domain;
    private String workflowID;
    private String runID;
    private String activityID;
    private String reason;
    private byte[] details;
    private String identity;
}
