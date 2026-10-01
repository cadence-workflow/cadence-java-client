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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.CreateScheduleResponse;
import com.uber.cadence.DescribeScheduleResponse;
import com.uber.cadence.ListSchedulesRequest;
import com.uber.cadence.ListSchedulesResponse;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.UpdateScheduleResponse;
import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import com.uber.cadence.client.schedule.ScheduleInitialState;
import com.uber.cadence.client.schedule.ScheduleListEntry;
import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ScheduleClientImplTest {

  private static final String DOMAIN = "test-domain";
  private static final String SCHEDULE_ID = "test-schedule";

  private IWorkflowService service;
  private ScheduleClientImpl client;

  @Before
  public void setUp() throws Exception {
    service = mock(IWorkflowService.class);
    when(service.CreateSchedule(any()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));
    when(service.UpdateSchedule(any()))
        .thenReturn(CompletableFuture.completedFuture(new UpdateScheduleResponse()));
    when(service.DescribeSchedule(any()))
        .thenReturn(CompletableFuture.completedFuture(minimalDescribeResponse()));
    client = new ScheduleClientImpl(service, DOMAIN);
  }

  // --- toThriftSpec ---

  @Test
  public void createSchedule_spec_cronExpression() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().setCronExpression("* * * * *").build(),
            minimalAction(),
            SchedulePolicies.newBuilder().build())
        .join();

    assertEquals("* * * * *", captor.getValue().getSpec().getCronExpression());
  }

  @Test
  public void createSchedule_spec_startEndTime_fullNanoPrecision() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    Instant start = Instant.ofEpochSecond(1_700_000_000L, 123_456_789L);
    Instant end = Instant.ofEpochSecond(1_800_000_000L, 987_654_321L);

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().setStartTime(start).setEndTime(end).build(),
            minimalAction(),
            SchedulePolicies.newBuilder().build())
        .join();

    com.uber.cadence.ScheduleSpec spec = captor.getValue().getSpec();
    assertEquals(
        start.getEpochSecond() * 1_000_000_000L + start.getNano(), spec.getStartTimeNano());
    assertEquals(end.getEpochSecond() * 1_000_000_000L + end.getNano(), spec.getEndTimeNano());
  }

  @Test
  public void createSchedule_spec_nullStartEnd_leaveZero() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build(),
            minimalAction(),
            SchedulePolicies.newBuilder().build())
        .join();

    com.uber.cadence.ScheduleSpec spec = captor.getValue().getSpec();
    assertEquals(0L, spec.getStartTimeNano());
    assertEquals(0L, spec.getEndTimeNano());
  }

  @Test
  public void createSchedule_spec_jitter() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().setJitter(Duration.ofSeconds(30)).build(),
            minimalAction(),
            SchedulePolicies.newBuilder().build())
        .join();

    assertEquals(30, captor.getValue().getSpec().getJitterInSeconds());
  }

  // --- toThriftAction / toThriftStartWorkflow ---

  @Test
  public void createSchedule_action_workflowTypeAndTaskList() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(
                    ScheduleAction.StartWorkflowAction.newBuilder()
                        .setWorkflowType("MyWorkflow")
                        .setTaskList("my-tl")
                        .build())
                .build(),
            SchedulePolicies.newBuilder().build())
        .join();

    com.uber.cadence.ScheduleStartWorkflowAction sw =
        captor.getValue().getAction().getStartWorkflow();
    assertEquals("MyWorkflow", sw.getWorkflowType().getName());
    assertEquals("my-tl", sw.getTaskList().getName());
  }

  @Test
  public void createSchedule_action_timeouts() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(
                    ScheduleAction.StartWorkflowAction.newBuilder()
                        .setWorkflowType("wf")
                        .setTaskList("tl")
                        .setExecutionStartToCloseTimeout(Duration.ofSeconds(120))
                        .setTaskStartToCloseTimeout(Duration.ofSeconds(10))
                        .build())
                .build(),
            SchedulePolicies.newBuilder().build())
        .join();

    com.uber.cadence.ScheduleStartWorkflowAction sw =
        captor.getValue().getAction().getStartWorkflow();
    assertEquals(120, sw.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(10, sw.getTaskStartToCloseTimeoutSeconds());
  }

  @Test
  public void createSchedule_action_retryPolicy() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    RetryOptions retry =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(60))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(5)
            .setExpiration(Duration.ofMinutes(10))
            .build();

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(
                    ScheduleAction.StartWorkflowAction.newBuilder()
                        .setWorkflowType("wf")
                        .setTaskList("tl")
                        .setRetryOptions(retry)
                        .build())
                .build(),
            SchedulePolicies.newBuilder().build())
        .join();

    com.uber.cadence.RetryPolicy rp =
        captor.getValue().getAction().getStartWorkflow().getRetryPolicy();
    assertNotNull(rp);
    assertEquals(1, rp.getInitialIntervalInSeconds());
    assertEquals(60, rp.getMaximumIntervalInSeconds());
    assertEquals(2.0, rp.getBackoffCoefficient(), 0.0);
    assertEquals(5, rp.getMaximumAttempts());
    assertEquals(600, rp.getExpirationIntervalInSeconds());
  }

  @Test
  public void createSchedule_action_memo() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    byte[] value = "hello".getBytes();
    Map<String, Object> memo = new HashMap<>();
    memo.put("key", value);

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(
                    ScheduleAction.StartWorkflowAction.newBuilder()
                        .setWorkflowType("wf")
                        .setTaskList("tl")
                        .setMemo(memo)
                        .build())
                .build(),
            SchedulePolicies.newBuilder().build())
        .join();

    assertArrayEquals(
        value, captor.getValue().getAction().getStartWorkflow().getMemo().getFields().get("key"));
  }

  @Test(expected = java.util.concurrent.CompletionException.class)
  public void createSchedule_action_memo_nonByteArrayValueThrows() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("key", "not-a-byte-array");

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(
                    ScheduleAction.StartWorkflowAction.newBuilder()
                        .setWorkflowType("wf")
                        .setTaskList("tl")
                        .setMemo(memo)
                        .build())
                .build(),
            SchedulePolicies.newBuilder().build())
        .join();
  }

  // --- toThriftPolicies ---

  @Test
  public void createSchedule_policies_overlapAndCatchUp() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().build(),
            minimalAction(),
            SchedulePolicies.newBuilder()
                .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW)
                .setCatchUpPolicy(ScheduleCatchUpPolicy.ONE)
                .setCatchUpWindow(Duration.ofMinutes(5))
                .setPauseOnFailure(true)
                .setBufferLimit(3)
                .setConcurrencyLimit(2)
                .build())
        .join();

    com.uber.cadence.SchedulePolicies p = captor.getValue().getPolicies();
    assertEquals(com.uber.cadence.ScheduleOverlapPolicy.SKIP_NEW, p.getOverlapPolicy());
    assertEquals(com.uber.cadence.ScheduleCatchUpPolicy.ONE, p.getCatchUpPolicy());
    assertEquals(300, p.getCatchUpWindowInSeconds());
    assertEquals(true, p.isPauseOnFailure());
    assertEquals(3, p.getBufferLimit());
    assertEquals(2, p.getConcurrencyLimit());
  }

  // --- updateSchedule overload ---

  @Test
  public void updateSchedule_cleanTypeOverload_setsFields() throws Exception {
    ArgumentCaptor<UpdateScheduleRequest> captor = forClass(UpdateScheduleRequest.class);
    when(service.UpdateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new UpdateScheduleResponse()));

    client
        .updateSchedule(
            SCHEDULE_ID,
            ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build(),
            minimalAction(),
            SchedulePolicies.newBuilder()
                .setOverlapPolicy(ScheduleOverlapPolicy.CONCURRENT)
                .build())
        .join();

    UpdateScheduleRequest req = captor.getValue();
    assertEquals(DOMAIN, req.getDomain());
    assertEquals(SCHEDULE_ID, req.getScheduleId());
    assertEquals("0 * * * *", req.getSpec().getCronExpression());
    assertEquals(
        com.uber.cadence.ScheduleOverlapPolicy.CONCURRENT, req.getPolicies().getOverlapPolicy());
  }

  // --- initialState ---

  @Test
  public void createSchedule_initialState_pausedWithReasonAndPausedBy() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    ScheduleInitialState initialState = new ScheduleInitialState(true, "deploying", "ci-bot");
    client.createSchedule(SCHEDULE_ID, null, minimalAction(), null, initialState).join();

    CreateScheduleRequest req = captor.getValue();
    assertNotNull(req.getState());
    assertEquals(true, req.getState().isPaused());
    assertNotNull(req.getState().getPauseInfo());
    assertEquals("deploying", req.getState().getPauseInfo().getReason());
    assertEquals("ci-bot", req.getState().getPauseInfo().getPausedBy());
  }

  @Test
  public void createSchedule_initialState_pausedNoPauseInfo() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID, null, minimalAction(), null, new ScheduleInitialState(true, null, null))
        .join();

    CreateScheduleRequest req = captor.getValue();
    assertNotNull(req.getState());
    assertEquals(true, req.getState().isPaused());
    assertNull(req.getState().getPauseInfo());
  }

  @Test
  public void createSchedule_initialState_null_sendsNoState() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client.createSchedule(SCHEDULE_ID, null, minimalAction(), null, null).join();

    assertNull(captor.getValue().getState());
  }

  @Test
  public void createSchedule_fourArg_sendsNoState() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client.createSchedule(SCHEDULE_ID, null, minimalAction(), null).join();

    assertNull(captor.getValue().getState());
  }

  @Test
  public void createSchedule_initialState_pausedWithReasonOnly() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID,
            null,
            minimalAction(),
            null,
            new ScheduleInitialState(true, "reason", null))
        .join();

    CreateScheduleRequest req = captor.getValue();
    assertNotNull(req.getState().getPauseInfo());
    assertEquals("reason", req.getState().getPauseInfo().getReason());
    assertNull(req.getState().getPauseInfo().getPausedBy());
  }

  @Test
  public void createSchedule_initialState_pausedWithPausedByOnly() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client
        .createSchedule(
            SCHEDULE_ID, null, minimalAction(), null, new ScheduleInitialState(true, null, "ci"))
        .join();

    CreateScheduleRequest req = captor.getValue();
    assertNotNull(req.getState().getPauseInfo());
    assertNull(req.getState().getPauseInfo().getReason());
    assertEquals("ci", req.getState().getPauseInfo().getPausedBy());
  }

  @Test
  public void createScheduleRequest_nullState_producesNoStateInProto() {
    com.uber.cadence.CreateScheduleRequest thrift =
        new com.uber.cadence.CreateScheduleRequest().setDomain(DOMAIN).setScheduleId(SCHEDULE_ID);

    com.uber.cadence.api.v1.CreateScheduleRequest proto =
        com.uber.cadence.internal.compatibility.proto.mappers.RequestMapper.createScheduleRequest(
            thrift);

    assertFalse(proto.hasState());
  }

  @Test
  public void createScheduleRequest_stateSerializesToProto() {
    com.uber.cadence.SchedulePauseInfo pi = new com.uber.cadence.SchedulePauseInfo();
    pi.setReason("deploying");
    pi.setPausedBy("ci-bot");
    com.uber.cadence.ScheduleState thriftState =
        new com.uber.cadence.ScheduleState().setPaused(true).setPauseInfo(pi);
    com.uber.cadence.CreateScheduleRequest thrift =
        new com.uber.cadence.CreateScheduleRequest()
            .setDomain(DOMAIN)
            .setScheduleId(SCHEDULE_ID)
            .setState(thriftState);

    com.uber.cadence.api.v1.CreateScheduleRequest proto =
        com.uber.cadence.internal.compatibility.proto.mappers.RequestMapper.createScheduleRequest(
            thrift);

    assertTrue(proto.getState().getPaused());
    assertEquals("deploying", proto.getState().getPauseInfo().getReason());
    assertEquals("ci-bot", proto.getState().getPauseInfo().getPausedBy());
  }

  // --- null handling ---

  @Test
  public void createSchedule_nullSpec_sendsNullSpec() throws Exception {
    ArgumentCaptor<CreateScheduleRequest> captor = forClass(CreateScheduleRequest.class);
    when(service.CreateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new CreateScheduleResponse()));

    client.createSchedule(SCHEDULE_ID, null, minimalAction(), null).join();

    assertNull(captor.getValue().getSpec());
    assertNull(captor.getValue().getPolicies());
  }

  // --- updateSchedule callback ---

  @Test
  public void updateSchedule_callback_describesAndSubmitsUpdatedSpec() throws Exception {
    ArgumentCaptor<UpdateScheduleRequest> captor = forClass(UpdateScheduleRequest.class);
    when(service.UpdateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new UpdateScheduleResponse()));

    ScheduleSpec newSpec = ScheduleSpec.newBuilder().setCronExpression("0 9 * * 1-5").build();
    client
        .updateSchedule(SCHEDULE_ID, current -> current.toBuilder().setSpec(newSpec).build())
        .join();

    UpdateScheduleRequest req = captor.getValue();
    assertEquals(DOMAIN, req.getDomain());
    assertEquals(SCHEDULE_ID, req.getScheduleId());
    assertEquals("0 9 * * 1-5", req.getSpec().getCronExpression());
  }

  @Test
  public void updateSchedule_callback_onlyChangedFieldsSentToServer() throws Exception {
    ArgumentCaptor<UpdateScheduleRequest> captor = forClass(UpdateScheduleRequest.class);
    when(service.UpdateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new UpdateScheduleResponse()));

    SchedulePolicies newPolicies =
        SchedulePolicies.newBuilder().setOverlapPolicy(ScheduleOverlapPolicy.BUFFER).build();
    client
        .updateSchedule(
            SCHEDULE_ID, current -> current.toBuilder().setPolicies(newPolicies).build())
        .join();

    UpdateScheduleRequest req = captor.getValue();
    // spec and action were not replaced by the callback, so they are not re-serialized
    assertNull(req.getSpec());
    assertNull(req.getAction());
    assertEquals(
        com.uber.cadence.ScheduleOverlapPolicy.BUFFER, req.getPolicies().getOverlapPolicy());
  }

  @Test
  public void updateSchedule_callback_noChanges_sendsNullFields() throws Exception {
    ArgumentCaptor<UpdateScheduleRequest> captor = forClass(UpdateScheduleRequest.class);
    when(service.UpdateSchedule(captor.capture()))
        .thenReturn(CompletableFuture.completedFuture(new UpdateScheduleResponse()));

    client.updateSchedule(SCHEDULE_ID, current -> current).join();

    UpdateScheduleRequest req = captor.getValue();
    assertNull(req.getSpec());
    assertNull(req.getAction());
    assertNull(req.getPolicies());
  }

  // --- listSchedules (stream) ---

  @Test
  public void listSchedules_eachCallReturnsIndependentStream() {
    com.uber.cadence.ScheduleListEntry e =
        new com.uber.cadence.ScheduleListEntry().setScheduleId("s1");
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e))))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(e))));

    assertEquals(1, client.listSchedules().count());
    assertEquals(1, client.listSchedules().count());
  }

  @Test
  public void listSchedules_noArg_usesDefaultPageSize() {
    ArgumentCaptor<ListSchedulesRequest> captor = forClass(ListSchedulesRequest.class);
    when(service.ListSchedules(captor.capture()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    client.listSchedules().forEach(e -> {});

    assertEquals(ScheduleListIterator.DEFAULT_PAGE_SIZE, captor.getValue().getPageSize());
  }

  @Test
  public void listSchedules_withPageSize_passesPageSizeToRequest() {
    ArgumentCaptor<ListSchedulesRequest> captor = forClass(ListSchedulesRequest.class);
    when(service.ListSchedules(captor.capture()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.emptyList())));

    client.listSchedules(42).forEach(e -> {});

    assertEquals(42, captor.getValue().getPageSize());
  }

  @Test
  public void listSchedules_stream_mapsEntriesToClientTypes() {
    com.uber.cadence.ScheduleListEntry thrift =
        new com.uber.cadence.ScheduleListEntry()
            .setScheduleId("sched-1")
            .setWorkflowType(new com.uber.cadence.WorkflowType().setName("MyWf"))
            .setState(new com.uber.cadence.ScheduleState().setPaused(true))
            .setCronExpression("0 9 * * 1-5");
    when(service.ListSchedules(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                new ListSchedulesResponse().setSchedules(Collections.singletonList(thrift))));

    ScheduleListEntry entry = client.listSchedules().findFirst().orElseThrow(AssertionError::new);
    assertEquals("sched-1", entry.getScheduleId());
    assertEquals("MyWf", entry.getWorkflowType());
    assertTrue(entry.isPaused());
    assertEquals("0 9 * * 1-5", entry.getCronExpression());
  }

  // --- helpers ---

  private static ScheduleAction minimalAction() {
    return ScheduleAction.newBuilder()
        .setStartWorkflow(
            ScheduleAction.StartWorkflowAction.newBuilder()
                .setWorkflowType("wf")
                .setTaskList("tl")
                .build())
        .build();
  }

  private static DescribeScheduleResponse minimalDescribeResponse() {
    com.uber.cadence.ScheduleSpec spec =
        new com.uber.cadence.ScheduleSpec().setCronExpression("0 * * * *");
    com.uber.cadence.ScheduleStartWorkflowAction swa =
        new com.uber.cadence.ScheduleStartWorkflowAction()
            .setWorkflowType(new com.uber.cadence.WorkflowType().setName("wf"))
            .setTaskList(new com.uber.cadence.TaskList().setName("tl"));
    com.uber.cadence.ScheduleAction action =
        new com.uber.cadence.ScheduleAction().setStartWorkflow(swa);
    com.uber.cadence.SchedulePolicies policies = new com.uber.cadence.SchedulePolicies();
    com.uber.cadence.ScheduleState state = new com.uber.cadence.ScheduleState().setPaused(false);
    com.uber.cadence.ScheduleInfo info = new com.uber.cadence.ScheduleInfo();
    return new DescribeScheduleResponse()
        .setSpec(spec)
        .setAction(action)
        .setPolicies(policies)
        .setState(state)
        .setInfo(info);
  }
}
