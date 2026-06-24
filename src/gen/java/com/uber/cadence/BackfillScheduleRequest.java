// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BackfillScheduleRequest {
  private String domain;
  private String scheduleId;
  private Instant startTime;
  private Instant endTime;
  private ScheduleOverlapPolicy overlapPolicy;
  private String backfillId;
}
