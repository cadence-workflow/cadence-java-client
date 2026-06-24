// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleInfo;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import com.uber.cadence.client.schedule.ScheduleState;
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
