// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UnpauseScheduleRequest {
  private String domain;
  private String scheduleId;
  private String reason;
  /** Nullable. If null, uses the schedule's configured catch-up policy. */
  private ScheduleCatchUpPolicy catchUpPolicy;
}
