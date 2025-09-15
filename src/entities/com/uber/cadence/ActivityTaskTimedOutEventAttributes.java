package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActivityTaskTimedOutEventAttributes {
  private byte[] details;
  private long scheduledEventId;
  private long startedEventId;
  private TimeoutType timeoutType;
  private String lastFailureReason;
  private byte[] lastFailureDetails;
}
