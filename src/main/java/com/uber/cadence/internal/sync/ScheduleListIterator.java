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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

final class ScheduleListIterator implements Iterator<ScheduleListEntry> {

  static final int DEFAULT_PAGE_SIZE = 100;

  private final IWorkflowService service;
  private final String domain;
  private final int pageSize;

  private List<ScheduleListEntry> buffer = Collections.emptyList();
  private int index = 0;
  private byte[] nextPageToken = null;
  private boolean exhausted = false;

  ScheduleListIterator(IWorkflowService service, String domain, int pageSize) {
    this.service = service;
    this.domain = domain;
    this.pageSize = pageSize;
  }

  @Override
  public boolean hasNext() {
    while (index >= buffer.size() && !exhausted) {
      fetchNextPage();
    }
    return index < buffer.size();
  }

  @Override
  public ScheduleListEntry next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return buffer.get(index++);
  }

  private void fetchNextPage() {
    ListSchedulesResponse response;
    try {
      response =
          service.ListSchedules(
                  new ListSchedulesRequest()
                      .setDomain(domain)
                      .setPageSize(pageSize)
                      .setNextPageToken(nextPageToken))
              .get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw cause instanceof RuntimeException
          ? (RuntimeException) cause
          : new RuntimeException(cause);
    }

    List<ScheduleListEntry> page = new ArrayList<>();
    if (response.getSchedules() != null) {
      for (com.uber.cadence.ScheduleListEntry e : response.getSchedules()) {
        String workflowType = e.getWorkflowType() != null ? e.getWorkflowType().getName() : null;
        boolean paused = e.getState() != null && e.getState().isPaused();
        page.add(
            new ScheduleListEntry(e.getScheduleId(), workflowType, paused, e.getCronExpression()));
      }
    }
    buffer = page;
    index = 0;

    byte[] token = response.getNextPageToken();
    nextPageToken = (token != null && token.length > 0) ? token : null;
    if (nextPageToken == null) {
      exhausted = true;
    }
  }
}
