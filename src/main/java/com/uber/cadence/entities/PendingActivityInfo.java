package com.uber.cadence.entities;

import lombok.Data;

@Data
public class PendingActivityInfo {
    private String activityID;
    private ActivityType activityType;
    private PendingActivityState state;
    private byte[] heartbeatDetails;
    private Long lastHeartbeatTimestamp;
    private Long lastStartedTimestamp;
    private int attempt;
    private int maximumAttempts;
    private Long scheduledTimestamp;
    private Long expirationTimestamp;
    private String lastFailureReason;
    private String lastWorkerIdentity;
    private byte[] lastFailureDetails;
    private String startedWorkerIdentity;
}
