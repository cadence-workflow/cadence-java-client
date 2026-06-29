package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScheduleSpec {
  private String cronExpression;
  private long startTimeNano;
  private long endTimeNano;
  private int jitterInSeconds;
}
