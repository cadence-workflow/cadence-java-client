package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PauseScheduleRequest {
  private String domain;
  private String scheduleId;
  private String reason;
  private String identity;
}
