package com.uber.cadence.entities;

import lombok.Data;

@Data
public class StickyExecutionAttributes {
  private TaskList workerTaskList;
  private Integer scheduleToStartTimeoutSeconds;
}
