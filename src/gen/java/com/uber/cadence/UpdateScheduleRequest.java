package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateScheduleRequest {
  private String domain;
  private String scheduleId;
  private ScheduleSpec spec;
  private ScheduleAction action;
  private SchedulePolicies policies;
  private SearchAttributes searchAttributes;
}
