package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DecisionTaskFailedEventAttributes {
  private long scheduledEventId;
  private long startedEventId;
  private DecisionTaskFailedCause cause;
  private byte[] details;
  private String identity;
  private String reason;
  private String baseRunId;
  private String newRunId;
  private long forkEventVersion;
  private String binaryChecksum;
  private String requestId;
}
