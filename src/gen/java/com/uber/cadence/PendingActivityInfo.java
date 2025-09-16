package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PendingActivityInfo {
  private String activityID;
  private ActivityType activityType;
  private PendingActivityState state;
  private byte[] heartbeatDetails;
  private long lastHeartbeatTimestamp;
  private long lastStartedTimestamp;
  private int attempt;
  private int maximumAttempts;
  private long scheduledTimestamp;
  private long expirationTimestamp;
  private String lastFailureReason;
  private String lastWorkerIdentity;
  private byte[] lastFailureDetails;
  private String startedWorkerIdentity;
}
