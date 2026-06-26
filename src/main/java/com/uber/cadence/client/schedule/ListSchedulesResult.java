/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.client.schedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    this.schedules =
        schedules == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(schedules));
    this.nextPageToken = nextPageToken == null ? null : nextPageToken.clone();
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
    return nextPageToken == null ? null : nextPageToken.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ListSchedulesResult)) return false;
    ListSchedulesResult that = (ListSchedulesResult) o;
    return Objects.equals(schedules, that.schedules)
        && Arrays.equals(nextPageToken, that.nextPageToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schedules, Arrays.hashCode(nextPageToken));
  }

  @Override
  public String toString() {
    return "ListSchedulesResult{"
        + "schedules="
        + schedules
        + ", nextPageToken.length="
        + (nextPageToken == null ? "null" : nextPageToken.length)
        + '}';
  }
}
