/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.sync;

import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.client.schedule.ScheduleListEntry;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Paginating iterator for schedule list. The first page RPC is deferred until the first {@link
 * #hasNext()} call. Subsequent pages are prefetched immediately after the current page's token is
 * received, so the wait at page boundaries is minimised to the remaining in-flight time rather than
 * a full round-trip.
 */
final class ScheduleListIterator implements Iterator<ScheduleListEntry> {

  static final int DEFAULT_PAGE_SIZE = 100;

  private final IWorkflowService service;
  private final String domain;
  private final int pageSize;

  private List<ScheduleListEntry> activeBuffer = Collections.emptyList();
  private int index = 0;
  private boolean started = false;
  // null means no further pages exist; non-null means a page is in flight or ready.
  private CompletableFuture<ListSchedulesResponse> nextPageFuture;

  ScheduleListIterator(IWorkflowService service, String domain, int pageSize) {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be > 0, got " + pageSize);
    }
    this.service = service;
    this.domain = domain;
    this.pageSize = pageSize;
  }

  @Override
  public boolean hasNext() {
    if (!started) {
      started = true;
      nextPageFuture = fetch(null);
    }
    if (index < activeBuffer.size()) {
      return true;
    }
    advance();
    return index < activeBuffer.size();
  }

  @Override
  public ScheduleListEntry next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return activeBuffer.get(index++);
  }

  private void advance() {
    while (nextPageFuture != null) {
      ListSchedulesResponse response = get(nextPageFuture);
      byte[] token = response.getNextPageToken();
      byte[] normalizedToken = (token != null && token.length > 0) ? token : null;
      // Kick off the next page immediately before processing this one.
      nextPageFuture = normalizedToken != null ? fetch(normalizedToken) : null;

      List<ScheduleListEntry> page = toEntries(response);
      if (!page.isEmpty() || nextPageFuture == null) {
        activeBuffer = page;
        index = 0;
        return;
      }
      // Empty middle page with a continuation token is a server-side anomaly; loop to skip it.
    }
  }

  private CompletableFuture<ListSchedulesResponse> fetch(byte[] token) {
    return service.ListSchedules(
        new ListSchedulesRequest().setDomain(domain).setPageSize(pageSize).setNextPageToken(token));
  }

  private static ListSchedulesResponse get(CompletableFuture<ListSchedulesResponse> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw cause instanceof RuntimeException
          ? (RuntimeException) cause
          : new RuntimeException(cause);
    }
  }

  private static List<ScheduleListEntry> toEntries(ListSchedulesResponse response) {
    List<com.uber.cadence.ScheduleListEntry> raw = response.getSchedules();
    if (raw == null) {
      return Collections.emptyList();
    }
    List<ScheduleListEntry> result = new ArrayList<>(raw.size());
    for (com.uber.cadence.ScheduleListEntry e : raw) {
      String workflowType = e.getWorkflowType() != null ? e.getWorkflowType().getName() : null;
      boolean paused = e.getState() != null && e.getState().isPaused();
      Map<String, Object> memo =
          e.getMemo() != null ? toObjectMap(e.getMemo().getFields()) : null;
      Map<String, Object> searchAttributes =
          e.getSearchAttributes() != null
              ? toObjectMap(e.getSearchAttributes().getIndexedFields())
              : null;
      result.add(
          new ScheduleListEntry(
              e.getScheduleId(), workflowType, paused, e.getCronExpression(), memo,
              searchAttributes));
    }
    return result;
  }

  private static Map<String, Object> toObjectMap(Map<String, ?> src) {
    if (src == null || src.isEmpty()) return null;
    Map<String, Object> result = new HashMap<>();
    src.forEach((k, v) -> result.put(k, v));
    return result;
  }
}
