// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ListSchedulesRequest {
  private String domain;
  private int pageSize;
  private byte[] nextPageToken;
}
