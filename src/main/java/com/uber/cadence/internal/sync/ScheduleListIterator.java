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

final class ScheduleListIterator implements Iterator<ScheduleListEntry> {

  private static final int PAGE_SIZE = 1000;

  private final IWorkflowService service;
  private final String domain;

  private List<ScheduleListEntry> buffer = Collections.emptyList();
  private int index = 0;
  private byte[] nextPageToken = null;
  private boolean exhausted = false;

  ScheduleListIterator(IWorkflowService service, String domain) {
    this.service = service;
    this.domain = domain;
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
    ListSchedulesResponse response =
        service.ListSchedules(
                new ListSchedulesRequest()
                    .setDomain(domain)
                    .setPageSize(PAGE_SIZE)
                    .setNextPageToken(nextPageToken))
            .join();

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
