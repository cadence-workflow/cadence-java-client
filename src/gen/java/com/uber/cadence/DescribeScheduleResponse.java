package com.uber.cadence;

import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DescribeScheduleResponse {
  private ScheduleSpec spec;
  private ScheduleAction action;
  private SchedulePolicies policies;
  private ScheduleState state;
  private ScheduleInfo info;
  private Memo memo;
  private SearchAttributes searchAttributes;
}
