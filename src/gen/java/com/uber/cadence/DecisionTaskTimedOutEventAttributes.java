package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DecisionTaskTimedOutEventAttributes {
  private long scheduledEventId;
  private long startedEventId;
  private TimeoutType timeoutType;
  private String baseRunId;
  private String newRunId;
  private long forkEventVersion;
  private String reason;
  private DecisionTaskTimedOutCause cause;
  private String requestId;
}
