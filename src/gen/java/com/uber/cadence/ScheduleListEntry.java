package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScheduleListEntry {
  private String scheduleId;
  private WorkflowType workflowType;
  private ScheduleState state;
  private String cronExpression;
}
