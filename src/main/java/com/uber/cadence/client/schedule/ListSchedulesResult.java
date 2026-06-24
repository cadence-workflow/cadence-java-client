// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

import java.util.List;

/**
 * Result of a {@link com.uber.cadence.client.WorkflowClient#listSchedules} call.
 *
 * <p>To page through all schedules:
 *
 * <pre>{@code
 * byte[] token = null;
 * do {
 *     ListSchedulesResult result = client.listSchedules(100, token);
 *     for (ScheduleListEntry entry : result.getSchedules()) {
 *         System.out.println(entry.getScheduleId());
 *     }
 *     token = result.getNextPageToken();
 * } while (token != null && token.length > 0);
 * }</pre>
 */
public final class ListSchedulesResult {

  private final List<ScheduleListEntry> schedules;
  private final byte[] nextPageToken;

  public ListSchedulesResult(List<ScheduleListEntry> schedules, byte[] nextPageToken) {
    this.schedules = schedules;
    this.nextPageToken = nextPageToken;
  }

  /** Schedules in this page. May be empty on the last page. */
  public List<ScheduleListEntry> getSchedules() {
    return schedules;
  }

  /**
   * Opaque token to pass as {@code nextPageToken} on the next {@code listSchedules} call. {@code
   * null} or empty byte array means no more pages.
   */
  public byte[] getNextPageToken() {
    return nextPageToken;
  }
}
