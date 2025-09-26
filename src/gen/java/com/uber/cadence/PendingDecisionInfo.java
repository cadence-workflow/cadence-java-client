package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PendingDecisionInfo {
  private PendingDecisionState state;
  private long scheduledTimestamp;
  private long startedTimestamp;
  private long attempt;
  private long originalScheduledTimestamp;
  private long scheduleID;
}
