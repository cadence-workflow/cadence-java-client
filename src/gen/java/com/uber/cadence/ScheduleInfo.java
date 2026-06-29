package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScheduleInfo {
  private long lastRunTimeNano;
  private long nextRunTimeNano;
  private long totalRuns;
  private long createTimeNano;
  private long lastUpdateTimeNano;
  private List<BackfillInfo> ongoingBackfills = new ArrayList<>();;
  private long missedRuns;
  private long skippedRuns;
}
