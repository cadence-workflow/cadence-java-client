package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrossClusterTaskInfo {
  private String domainID;
  private String workflowID;
  private String runID;
  private CrossClusterTaskType taskType;
  private int taskState;
  private long taskID;
  private long visibilityTimestamp;
}
