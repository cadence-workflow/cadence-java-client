package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondActivityTaskFailedRequest {
    private byte[] taskToken;
    private String reason;
    private byte[] details;
    private String identity;
}
