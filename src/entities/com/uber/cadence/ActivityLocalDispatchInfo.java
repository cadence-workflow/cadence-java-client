package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActivityLocalDispatchInfo {
  private String activityId;
  private long scheduledTimestamp;
  private long startedTimestamp;
  private long scheduledTimestampOfThisAttempt;
  private byte[] taskToken;
}
