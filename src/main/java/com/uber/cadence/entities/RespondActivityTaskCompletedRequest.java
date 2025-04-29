package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RespondActivityTaskCompletedRequest {
    private byte[] taskToken;
    private byte[] result;
    private String identity;
}
