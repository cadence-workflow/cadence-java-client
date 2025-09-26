package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TaskListStatus {
  private long backlogCountHint;
  private long readLevel;
  private long ackLevel;
  private double ratePerSecond;
  private TaskIDBlock taskIDBlock;
  private Map<String, IsolationGroupMetrics> isolationGroupMetrics;
  private double newTasksPerSecond;
}
