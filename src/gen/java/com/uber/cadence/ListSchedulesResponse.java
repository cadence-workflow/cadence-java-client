// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import com.uber.cadence.client.schedule.ScheduleListEntry;
import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListSchedulesResponse {
  private List<ScheduleListEntry> schedules = new ArrayList<>();
  private byte[] nextPageToken;
}
