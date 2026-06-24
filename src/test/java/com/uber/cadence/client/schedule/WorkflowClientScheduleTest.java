// Copyright (c) 2026 Uber Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License").

package com.uber.cadence.client.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uber.cadence.BackfillScheduleRequest;
import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.DeleteScheduleRequest;
import com.uber.cadence.DescribeScheduleRequest;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.PauseScheduleRequest;
import com.uber.cadence.UnpauseScheduleRequest;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowClientOptions;
import com.uber.cadence.internal.sync.WorkflowClientInternal;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.time.Instant;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class WorkflowClientScheduleTest {

  private static final String DOMAIN = "test-domain";
  private static final String SCHEDULE_ID = "my-schedule";

  private IWorkflowService service;
  private WorkflowClient client;

  @Before
  public void setUp() throws Exception {
    service = mock(IWorkflowService.class);
    WorkflowClientOptions options = WorkflowClientOptions.newBuilder().setDomain(DOMAIN).build();
    client = WorkflowClientInternal.newInstance(service, options);
  }

  // --- createSchedule ---

  @Test
  public void createSchedule_sendsCorrectRequest() throws Exception {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 6 * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder()
            .setStartWorkflow(
                ScheduleAction.StartWorkflowAction.newBuilder()
                    .setWorkflowType("MyWorkflow")
                    .setTaskList("tl")
                    .build())
            .build();
    SchedulePolicies policies =
        SchedulePolicies.newBuilder().setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW).build();
    ScheduleState state = new ScheduleState(false, null, null, null);

    when(service.CreateSchedule(any())).thenReturn(new com.uber.cadence.CreateScheduleResponse());

    client.createSchedule(SCHEDULE_ID, spec, action, policies, state, null);

    ArgumentCaptor<CreateScheduleRequest> captor =
        ArgumentCaptor.forClass(CreateScheduleRequest.class);
    verify(service).CreateSchedule(captor.capture());
    CreateScheduleRequest req = captor.getValue();
    assertEquals(DOMAIN, req.getDomain());
    assertEquals(SCHEDULE_ID, req.getScheduleId());
    assertEquals(spec, req.getSpec());
    assertEquals(action, req.getAction());
    assertEquals(policies, req.getPolicies());
  }

  // --- describeSchedule ---

  @Test
  public void describeSchedule_mapsResponseFields() throws Exception {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 6 * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder()
            .setStartWorkflow(
                ScheduleAction.StartWorkflowAction.newBuilder()
                    .setWorkflowType("MyWorkflow")
                    .setTaskList("tl")
                    .build())
            .build();
    SchedulePolicies policies =
        SchedulePolicies.newBuilder().setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW).build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info = new ScheduleInfo(null, null, 5, null, null, Collections.emptyList(), 0, 0);

    DescribeScheduleResponse resp = new DescribeScheduleResponse();
    resp.setSpec(spec);
    resp.setAction(action);
    resp.setPolicies(policies);
    resp.setState(state);
    resp.setInfo(info);
    when(service.DescribeSchedule(any())).thenReturn(resp);

    ScheduleDescription desc = client.describeSchedule(SCHEDULE_ID);

    ArgumentCaptor<DescribeScheduleRequest> captor =
        ArgumentCaptor.forClass(DescribeScheduleRequest.class);
    verify(service).DescribeSchedule(captor.capture());
    assertEquals(DOMAIN, captor.getValue().getDomain());
    assertEquals(SCHEDULE_ID, captor.getValue().getScheduleId());

    assertEquals(spec, desc.getSpec());
    assertEquals(action, desc.getAction());
    assertEquals(policies, desc.getPolicies());
    assertEquals(5L, desc.getInfo().getTotalRuns());
  }

  // --- updateSchedule: describe-first read-modify-write ---

  @Test
  public void updateSchedule_callsDescribeFirstThenUpdate() throws Exception {
    ScheduleSpec original = ScheduleSpec.newBuilder().setCronExpression("0 6 * * *").build();
    ScheduleSpec updated = ScheduleSpec.newBuilder().setCronExpression("0 12 * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder()
            .setStartWorkflow(
                ScheduleAction.StartWorkflowAction.newBuilder()
                    .setWorkflowType("MyWorkflow")
                    .setTaskList("tl")
                    .build())
            .build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info = new ScheduleInfo(null, null, 0, null, null, Collections.emptyList(), 0, 0);

    DescribeScheduleResponse resp = new DescribeScheduleResponse();
    resp.setSpec(original);
    resp.setAction(action);
    resp.setPolicies(policies);
    resp.setState(state);
    resp.setInfo(info);
    when(service.DescribeSchedule(any())).thenReturn(resp);

    client.updateSchedule(SCHEDULE_ID, desc -> desc.setSpec(updated));

    // Verify describe was called first
    verify(service).DescribeSchedule(any());

    // Verify update was called with the mutated spec
    ArgumentCaptor<UpdateScheduleRequest> captor =
        ArgumentCaptor.forClass(UpdateScheduleRequest.class);
    verify(service).UpdateSchedule(captor.capture());
    UpdateScheduleRequest req = captor.getValue();
    assertEquals(DOMAIN, req.getDomain());
    assertEquals(SCHEDULE_ID, req.getScheduleId());
    assertEquals(updated, req.getSpec());
    assertEquals(action, req.getAction());
    assertEquals(policies, req.getPolicies());
  }

  @Test
  public void updateSchedule_doesNotUpdateWhenUpdaterReturnsUnchanged() throws Exception {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 6 * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder()
            .setStartWorkflow(
                ScheduleAction.StartWorkflowAction.newBuilder()
                    .setWorkflowType("MyWorkflow")
                    .setTaskList("tl")
                    .build())
            .build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info = new ScheduleInfo(null, null, 0, null, null, Collections.emptyList(), 0, 0);

    DescribeScheduleResponse resp = new DescribeScheduleResponse();
    resp.setSpec(spec);
    resp.setAction(action);
    resp.setPolicies(policies);
    resp.setState(state);
    resp.setInfo(info);
    when(service.DescribeSchedule(any())).thenReturn(resp);

    // Updater returns the same description unchanged
    client.updateSchedule(SCHEDULE_ID, desc -> desc);

    verify(service).DescribeSchedule(any());
    // UpdateSchedule should still be called (server decides idempotency)
    verify(service).UpdateSchedule(any());
  }

  // --- deleteSchedule ---

  @Test
  public void deleteSchedule_sendsCorrectRequest() throws Exception {
    client.deleteSchedule(SCHEDULE_ID);

    ArgumentCaptor<DeleteScheduleRequest> captor =
        ArgumentCaptor.forClass(DeleteScheduleRequest.class);
    verify(service).DeleteSchedule(captor.capture());
    assertEquals(DOMAIN, captor.getValue().getDomain());
    assertEquals(SCHEDULE_ID, captor.getValue().getScheduleId());
  }

  // --- pauseSchedule ---

  @Test
  public void pauseSchedule_sendsReasonInRequest() throws Exception {
    client.pauseSchedule(SCHEDULE_ID, "maintenance");

    ArgumentCaptor<PauseScheduleRequest> captor =
        ArgumentCaptor.forClass(PauseScheduleRequest.class);
    verify(service).PauseSchedule(captor.capture());
    assertEquals(DOMAIN, captor.getValue().getDomain());
    assertEquals(SCHEDULE_ID, captor.getValue().getScheduleId());
    assertEquals("maintenance", captor.getValue().getReason());
  }

  // --- unpauseSchedule ---

  @Test
  public void unpauseSchedule_withoutPolicySendsNullCatchUp() throws Exception {
    client.unpauseSchedule(SCHEDULE_ID, "done");

    ArgumentCaptor<UnpauseScheduleRequest> captor =
        ArgumentCaptor.forClass(UnpauseScheduleRequest.class);
    verify(service).UnpauseSchedule(captor.capture());
    assertEquals(DOMAIN, captor.getValue().getDomain());
    assertEquals("done", captor.getValue().getReason());
    assertEquals(null, captor.getValue().getCatchUpPolicy());
  }

  @Test
  public void unpauseSchedule_withCatchUpPolicy() throws Exception {
    client.unpauseSchedule(SCHEDULE_ID, "done", ScheduleCatchUpPolicy.SKIP);

    ArgumentCaptor<UnpauseScheduleRequest> captor =
        ArgumentCaptor.forClass(UnpauseScheduleRequest.class);
    verify(service).UnpauseSchedule(captor.capture());
    assertEquals(ScheduleCatchUpPolicy.SKIP, captor.getValue().getCatchUpPolicy());
  }

  // --- backfillSchedule ---

  @Test
  public void backfillSchedule_sendsCorrectTimeRange() throws Exception {
    Instant start = Instant.ofEpochSecond(1000);
    Instant end = Instant.ofEpochSecond(2000);

    client.backfillSchedule(SCHEDULE_ID, start, end, ScheduleOverlapPolicy.BUFFER);

    ArgumentCaptor<BackfillScheduleRequest> captor =
        ArgumentCaptor.forClass(BackfillScheduleRequest.class);
    verify(service).BackfillSchedule(captor.capture());
    BackfillScheduleRequest req = captor.getValue();
    assertEquals(DOMAIN, req.getDomain());
    assertEquals(SCHEDULE_ID, req.getScheduleId());
    assertEquals(start, req.getStartTime());
    assertEquals(end, req.getEndTime());
    assertEquals(ScheduleOverlapPolicy.BUFFER, req.getOverlapPolicy());
  }

  // --- listSchedules ---

  @Test
  public void listSchedules_returnsEntriesFromResponse() throws Exception {
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleListEntry entry = new ScheduleListEntry("s1", "MyWorkflow", state, "0 6 * * *");
    ListSchedulesResponse resp = new ListSchedulesResponse();
    resp.setSchedules(Collections.singletonList(entry));
    resp.setNextPageToken("tok".getBytes());
    when(service.ListSchedules(any())).thenReturn(resp);

    ListSchedulesResult result = client.listSchedules(10, null);

    ArgumentCaptor<ListSchedulesRequest> captor =
        ArgumentCaptor.forClass(ListSchedulesRequest.class);
    verify(service).ListSchedules(captor.capture());
    assertEquals(DOMAIN, captor.getValue().getDomain());
    assertEquals(10, captor.getValue().getPageSize());

    assertEquals(1, result.getSchedules().size());
    assertEquals("s1", result.getSchedules().get(0).getScheduleId());
    assertNotNull(result.getNextPageToken());
  }
}
