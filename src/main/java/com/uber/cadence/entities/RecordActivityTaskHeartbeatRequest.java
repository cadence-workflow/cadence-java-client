package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RecordActivityTaskHeartbeatRequest {
    private byte[] taskToken;
    private byte[] details;
    private String identity;
}
