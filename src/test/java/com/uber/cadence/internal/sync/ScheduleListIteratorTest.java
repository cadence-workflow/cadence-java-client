package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import java.util.Arrays;
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

    Iterator<ScheduleListEntry> it = new ScheduleListIterator(service, DOMAIN);

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

    Iterator<ScheduleListEntry> it = new ScheduleListIterator(service, DOMAIN);

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

    Iterator<ScheduleListEntry> it = new ScheduleListIterator(service, DOMAIN);
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

    assertFalse(new ScheduleListIterator(service, DOMAIN).hasNext());
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

    Iterator<ScheduleListEntry> it = new ScheduleListIterator(service, DOMAIN);
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

    assertFalse(new ScheduleListIterator(service, DOMAIN).hasNext());
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

    assertFalse(new ScheduleListIterator(service, DOMAIN).next().isPaused());
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
                new ListSchedulesResponse().setSchedules(Arrays.asList(e2))));

    Iterator<ScheduleListEntry> it = new ScheduleListIterator(service, DOMAIN);
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

    new ScheduleListIterator(service, DOMAIN).next();
  }

  @Test
  public void listSchedules_iterable_canIterateTwice() {
    com.uber.cadence.ScheduleListEntry e =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s1");
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e))))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e))));

    ScheduleClientImpl client = new ScheduleClientImpl(service, DOMAIN);
    Iterable<ScheduleListEntry> iterable = client.listSchedules();

    int count1 = 0;
    for (ScheduleListEntry ignored : iterable) count1++;
    int count2 = 0;
    for (ScheduleListEntry ignored : iterable) count2++;

    assertEquals(1, count1);
    assertEquals(1, count2);
  }
}
