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
package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.client.schedule.ScheduleListEntry;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ScheduleListIteratorTest {

  private static final String DOMAIN = "test-domain";

  private IWorkflowService service;

  @Before
  public void setUp() {
    service = mock(IWorkflowService.class);
  }

  // --- eager prefetch ---

  @Test
  public void constructor_prefetchesFirstPageEagerly() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);

    // First RPC must fire at construction time, before any hasNext()/next() call.
    verify(service, times(1)).ListSchedules(any());
  }

  // --- basic iteration ---

  @Test
  public void singlePage_returnsAllEntries() {
    com.uber.cadence.ScheduleListEntry thrift =
        new com.uber.cadence.ScheduleListEntry()
            .setScheduleId("s1")
            .setWorkflowType(new com.uber.cadence.WorkflowType().setName("Wf"))
            .setState(new com.uber.cadence.ScheduleState().setPaused(false))
            .setCronExpression("* * * * *");
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(thrift))));

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);

    assertTrue(it.hasNext());
    ScheduleListEntry entry = it.next();
    assertEquals("s1", entry.getScheduleId());
    assertEquals("Wf", entry.getWorkflowType());
    assertFalse(entry.isPaused());
    assertEquals("* * * * *", entry.getCronExpression());
    assertFalse(it.hasNext());
  }

  @Test
  public void multiPage_fetchesUntilTokenNull() {
    com.uber.cadence.ScheduleListEntry e1 =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s1");
    com.uber.cadence.ScheduleListEntry e2 =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s2");

    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(Collections.singletonList(e1))
                    .setNextPageToken(new byte[] {1})))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e2))));

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);

    assertEquals("s1", it.next().getScheduleId());
    assertEquals("s2", it.next().getScheduleId());
    assertFalse(it.hasNext());
    verify(service, times(2)).ListSchedules(any());
  }

  @Test
  public void emptyToken_treatedAsLastPage() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(
                        Collections.singletonList(
                            new com.uber.cadence.ScheduleListEntry().setScheduleId("s1")))
                    .setNextPageToken(new byte[0])));

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);
    it.next();
    assertFalse(it.hasNext());
    verify(service, times(1)).ListSchedules(any());
  }

  @Test
  public void emptyFirstPage_returnsNoEntries() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    assertFalse(
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE)
            .hasNext());
  }

  @Test
  public void multiPage_tokenPassedToNextRequest() {
    ArgumentCaptor<ListSchedulesRequest> captor = forClass(ListSchedulesRequest.class);
    byte[] token = {9, 8};
    when(service.ListSchedules(captor.capture()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(
                        Collections.singletonList(
                            new com.uber.cadence.ScheduleListEntry().setScheduleId("s1")))
                    .setNextPageToken(token)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);
    it.next();
    it.hasNext();

    assertEquals(DOMAIN, captor.getAllValues().get(0).getDomain());
    assertEquals(token, captor.getAllValues().get(1).getNextPageToken());
  }

  @Test
  public void nullSchedulesList_treatedAsEmptyPage() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(new ListSchedulesResponse().setSchedules(null)));

    assertFalse(
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE)
            .hasNext());
  }

  @Test
  public void nullState_notPaused() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(
                        Collections.singletonList(
                            new com.uber.cadence.ScheduleListEntry()
                                .setScheduleId("s")
                                .setState(null)))));

    assertFalse(
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE)
            .next()
            .isPaused());
  }

  @Test
  public void multiPageEmptyMiddlePage_skippedTransparently() {
    com.uber.cadence.ScheduleListEntry e1 =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s1");
    com.uber.cadence.ScheduleListEntry e2 =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s2");
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(Collections.singletonList(e1))
                    .setNextPageToken(new byte[] {1})))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse()
                    .setSchedules(Collections.emptyList())
                    .setNextPageToken(new byte[] {2})))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e2))));

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);
    assertEquals("s1", it.next().getScheduleId());
    assertEquals("s2", it.next().getScheduleId());
    assertFalse(it.hasNext());
  }

  @Test(expected = NoSuchElementException.class)
  public void next_afterExhausted_throws() {
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE).next();
  }

  // --- pageSize validation ---

  @Test(expected = IllegalArgumentException.class)
  public void constructor_zeroPageSize_throws() {
    new ScheduleListIterator(service, DOMAIN, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructor_negativePageSize_throws() {
    new ScheduleListIterator(service, DOMAIN, -1);
  }

  // --- exception propagation ---

  @Test
  public void fetchNextPage_runtimeExceptionPassedThroughDirectly() {
    RuntimeException cause = new RuntimeException("service down");
    CompletableFuture<ListSchedulesResponse> failed = new CompletableFuture<>();
    failed.completeExceptionally(cause);
    when(service.ListSchedules(any())).thenReturn(failed);

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);
    try {
      it.hasNext();
      fail("expected RuntimeException");
    } catch (RuntimeException e) {
      assertSame("RuntimeException cause must be re-thrown directly, not double-wrapped", cause, e);
    }
  }

  @Test
  public void fetchNextPage_checkedExceptionWrappedInRuntimeException() {
    Exception checked = new Exception("checked");
    CompletableFuture<ListSchedulesResponse> failed = new CompletableFuture<>();
    failed.completeExceptionally(checked);
    when(service.ListSchedules(any())).thenReturn(failed);

    Iterator<ScheduleListEntry> it =
        new ScheduleListIterator(service, DOMAIN, ScheduleListIterator.DEFAULT_PAGE_SIZE);
    try {
      it.hasNext();
      fail("expected RuntimeException");
    } catch (RuntimeException e) {
      assertSame(checked, e.getCause());
    }
  }
}
