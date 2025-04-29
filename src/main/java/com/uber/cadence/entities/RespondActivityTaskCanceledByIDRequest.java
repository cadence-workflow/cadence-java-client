package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondActivityTaskCanceledByIDRequest {
    private String domain;
    private String workflowID;
    private String runID;
    private String activityID;
    private byte[] details;
    private String identity;
}
