package com.uber.cadence.entities;

import lombok.Data;

@Data
public class RecordActivityTaskHeartbeatResponse {
    private boolean cancelRequested;
}
