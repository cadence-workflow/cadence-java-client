package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FailoverInfo {
  private long failoverVersion;
  private long failoverStartTimestamp;
  private long failoverExpireTimestamp;
  private int completedShardCount;
  private List<Integer> pendingShards;
}
