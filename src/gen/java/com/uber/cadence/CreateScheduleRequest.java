// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateScheduleRequest {
  private String domain;
  private String scheduleId;
  private ScheduleSpec spec;
  private ScheduleAction action;
  private SchedulePolicies policies;
  private Memo memo;
  private SearchAttributes searchAttributes;
}
