package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RemoveTaskRequest {
  private int shardID;
  private int type;
  private long taskID;
  private long visibilityTimestamp;
  private String clusterName;
}
