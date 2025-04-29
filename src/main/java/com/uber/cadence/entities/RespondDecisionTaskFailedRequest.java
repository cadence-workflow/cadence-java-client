package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondDecisionTaskFailedRequest {
    private byte[] taskToken;
    private DecisionTaskFailedCause cause;
    private byte[] details;
    private String binaryChecksum;
    private String identity;
}
