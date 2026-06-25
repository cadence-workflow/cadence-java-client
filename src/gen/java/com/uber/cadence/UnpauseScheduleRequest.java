package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnpauseScheduleRequest {
  private String domain;
  private String scheduleId;
  private String reason;
  private ScheduleCatchUpPolicy catchUpPolicy;
}
