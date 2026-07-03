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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.cadence.CreateScheduleRequest;
import com.uber.cadence.CreateScheduleResponse;
import com.uber.cadence.UpdateScheduleRequest;
import com.uber.cadence.UpdateScheduleResponse;
import com.uber.cadence.client.schedule.ScheduleAction;
import com.uber.cadence.client.schedule.ScheduleCatchUpPolicy;
import com.uber.cadence.client.schedule.ScheduleOverlapPolicy;
import com.uber.cadence.client.schedule.SchedulePolicies;
import com.uber.cadence.client.schedule.ScheduleSpec;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.time.Duration;
import java.time.Instant;
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
}
