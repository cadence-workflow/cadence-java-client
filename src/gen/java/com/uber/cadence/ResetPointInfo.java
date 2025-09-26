package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResetPointInfo {
  private String binaryChecksum;
  private String runId;
  private long firstDecisionCompletedId;
  private long createdTimeNano;
  private long expiringTimeNano;
  private boolean resettable;
}
