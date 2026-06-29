package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BackfillScheduleRequest {
  private String domain;
  private String scheduleId;
  private long startTimeNano;
  private long endTimeNano;
  private ScheduleOverlapPolicy overlapPolicy;
  private String backfillId;
}
