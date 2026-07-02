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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.uber.cadence.client.ScheduleBackfill;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ScheduleTypesTest {

  @Test(expected = IllegalStateException.class)
  public void startWorkflowAction_build_requiresWorkflowType() {
    ScheduleAction.StartWorkflowAction.newBuilder().setTaskList("tl").build();
  }

  @Test(expected = IllegalStateException.class)
  public void startWorkflowAction_build_requiresTaskList() {
    ScheduleAction.StartWorkflowAction.newBuilder().setWorkflowType("wf").build();
  }

  @Test(expected = IllegalStateException.class)
  public void scheduleAction_build_requiresStartWorkflow() {
    ScheduleAction.newBuilder().build();
  }

  @Test
  public void startWorkflowAction_getters() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("k", "v");
    Map<String, Object> sa = new HashMap<>();
    sa.put("key", "val");

    ScheduleAction.StartWorkflowAction action =
        ScheduleAction.StartWorkflowAction.newBuilder()
            .setWorkflowType("MyWf")
            .setTaskList("tl")
            .setInput(new byte[] {1, 2})
            .setWorkflowIdPrefix("prefix")
            .setExecutionStartToCloseTimeout(Duration.ofHours(1))
            .setTaskStartToCloseTimeout(Duration.ofSeconds(10))
            .setMemo(memo)
            .setSearchAttributes(sa)
            .build();

    assertEquals("MyWf", action.getWorkflowType());
    assertEquals("tl", action.getTaskList());
    assertArrayEquals(new byte[] {1, 2}, action.getInput());
    assertEquals("prefix", action.getWorkflowIdPrefix());
    assertEquals(Duration.ofHours(1), action.getExecutionStartToCloseTimeout());
    assertEquals(Duration.ofSeconds(10), action.getTaskStartToCloseTimeout());
    assertNull(action.getRetryOptions());
    assertEquals(1, action.getMemo().size());
    assertEquals(1, action.getSearchAttributes().size());
  }

  @Test
  public void startWorkflowAction_toString() {
    ScheduleAction.StartWorkflowAction action =
        minimalStartWorkflowAction().setWorkflowIdPrefix("pfx").build();
    String s = action.toString();
    assertTrue(s.contains("MyWorkflow"));
    assertTrue(s.contains("my-task-list"));
    assertTrue(s.contains("pfx"));
  }

  @Test
  public void startWorkflowAction_inputNotAliasedOnSet() {
    byte[] original = {1, 2, 3};
    ScheduleAction.StartWorkflowAction action =
        minimalStartWorkflowAction().setInput(original).build();
    original[0] = 99;
    assertEquals(1, action.getInput()[0]);
  }

  @Test
  public void startWorkflowAction_inputNotExposedOnGet() {
    ScheduleAction.StartWorkflowAction action =
        minimalStartWorkflowAction().setInput(new byte[] {1, 2, 3}).build();
    action.getInput()[0] = 99;
    assertEquals(1, action.getInput()[0]);
  }

  @Test
  public void startWorkflowAction_toBuilder_inputIsIndependent() {
    ScheduleAction.StartWorkflowAction original =
        minimalStartWorkflowAction().setInput(new byte[] {10, 20, 30}).build();
    ScheduleAction.StartWorkflowAction copy = original.toBuilder().build();
    assertNotSame(original.getInput(), copy.getInput());
    assertArrayEquals(original.getInput(), copy.getInput());
  }

  @Test
  public void startWorkflowAction_nullInputRoundTrips() {
    ScheduleAction.StartWorkflowAction action = minimalStartWorkflowAction().build();
    assertNull(action.getInput());
    assertNull(action.toBuilder().build().getInput());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void startWorkflowAction_memoIsUnmodifiable() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("k", "v");
    minimalStartWorkflowAction().setMemo(memo).build().getMemo().put("x", "boom");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void startWorkflowAction_searchAttributesIsUnmodifiable() {
    Map<String, Object> sa = new HashMap<>();
    sa.put("k", "v");
    minimalStartWorkflowAction()
        .setSearchAttributes(sa)
        .build()
        .getSearchAttributes()
        .put("x", "y");
  }

  @Test
  public void startWorkflowAction_memoNotAliasedOnSet() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("k", "v");
    ScheduleAction.StartWorkflowAction action = minimalStartWorkflowAction().setMemo(memo).build();
    memo.put("extra", "boom");
    assertEquals(1, action.getMemo().size());
  }

  @Test
  public void startWorkflowAction_equals() {
    ScheduleAction.StartWorkflowAction a =
        minimalStartWorkflowAction().setInput(new byte[] {1, 2}).build();
    ScheduleAction.StartWorkflowAction b =
        minimalStartWorkflowAction().setInput(new byte[] {1, 2}).build();
    assertEquals(a, b);
    assertEquals(b, a);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void startWorkflowAction_notEqualOnDifferentInput() {
    assertNotEquals(
        minimalStartWorkflowAction().setInput(new byte[] {1}).build(),
        minimalStartWorkflowAction().setInput(new byte[] {2}).build());
  }

  @Test
  public void startWorkflowAction_notEqualOnDifferentMemo() {
    assertNotEquals(
        minimalStartWorkflowAction().setMemo(Collections.singletonMap("k", "v")).build(),
        minimalStartWorkflowAction().build());
  }

  @Test
  public void scheduleAction_toBuilder() {
    ScheduleAction.StartWorkflowAction swa = minimalStartWorkflowAction().build();
    ScheduleAction action = ScheduleAction.newBuilder().setStartWorkflow(swa).build();
    ScheduleAction copy = action.toBuilder().build();

    assertEquals(swa, action.getStartWorkflow());
    assertEquals(action, copy);
    assertEquals(action.hashCode(), copy.hashCode());
    assertTrue(action.toString().contains("StartWorkflowAction"));
  }

  @Test
  public void scheduleSpec_getters() {
    Instant start = Instant.ofEpochSecond(1000);
    Instant end = Instant.ofEpochSecond(2000);
    ScheduleSpec spec =
        ScheduleSpec.newBuilder()
            .setCronExpression("0 6 * * *")
            .setStartTime(start)
            .setEndTime(end)
            .setJitter(Duration.ofMinutes(5))
            .build();

    assertEquals("0 6 * * *", spec.getCronExpression());
    assertEquals(start, spec.getStartTime());
    assertEquals(end, spec.getEndTime());
    assertEquals(Duration.ofMinutes(5), spec.getJitter());
  }

  @Test
  public void scheduleSpec_toBuilder() {
    ScheduleSpec original =
        ScheduleSpec.newBuilder()
            .setCronExpression("0 6 * * *")
            .setStartTime(Instant.EPOCH)
            .setJitter(Duration.ofMinutes(10))
            .build();
    ScheduleSpec copy = original.toBuilder().build();
    assertEquals(original, copy);
    assertEquals(original.hashCode(), copy.hashCode());
  }

  @Test
  public void scheduleSpec_notEqualOnDifferentCron() {
    assertNotEquals(
        ScheduleSpec.newBuilder().setCronExpression("0 6 * * *").build(),
        ScheduleSpec.newBuilder().setCronExpression("0 9 * * *").build());
  }

  @Test
  public void scheduleSpec_toString() {
    assertTrue(
        ScheduleSpec.newBuilder()
            .setCronExpression("0 6 * * *")
            .build()
            .toString()
            .contains("0 6 * * *"));
  }

  @Test
  public void schedulePolicies_getters() {
    SchedulePolicies p =
        SchedulePolicies.newBuilder()
            .setOverlapPolicy(ScheduleOverlapPolicy.BUFFER)
            .setCatchUpPolicy(ScheduleCatchUpPolicy.ALL)
            .setCatchUpWindow(Duration.ofHours(2))
            .setPauseOnFailure(true)
            .setBufferLimit(10)
            .setConcurrencyLimit(5)
            .build();

    assertEquals(ScheduleOverlapPolicy.BUFFER, p.getOverlapPolicy());
    assertEquals(ScheduleCatchUpPolicy.ALL, p.getCatchUpPolicy());
    assertEquals(Duration.ofHours(2), p.getCatchUpWindow());
    assertTrue(p.isPauseOnFailure());
    assertEquals(10, p.getBufferLimit());
    assertEquals(5, p.getConcurrencyLimit());
  }

  @Test
  public void schedulePolicies_toBuilder() {
    SchedulePolicies original =
        SchedulePolicies.newBuilder()
            .setOverlapPolicy(ScheduleOverlapPolicy.CONCURRENT)
            .setPauseOnFailure(true)
            .setBufferLimit(3)
            .build();
    SchedulePolicies copy = original.toBuilder().build();
    assertEquals(original, copy);
    assertEquals(original.hashCode(), copy.hashCode());
  }

  @Test
  public void schedulePolicies_equals() {
    SchedulePolicies a =
        SchedulePolicies.newBuilder()
            .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW)
            .setCatchUpPolicy(ScheduleCatchUpPolicy.ONE)
            .setPauseOnFailure(true)
            .setBufferLimit(5)
            .setConcurrencyLimit(3)
            .build();
    SchedulePolicies b =
        SchedulePolicies.newBuilder()
            .setOverlapPolicy(ScheduleOverlapPolicy.SKIP_NEW)
            .setCatchUpPolicy(ScheduleCatchUpPolicy.ONE)
            .setPauseOnFailure(true)
            .setBufferLimit(5)
            .setConcurrencyLimit(3)
            .build();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void schedulePolicies_notEqualOnDifferentPauseOnFailure() {
    assertNotEquals(
        SchedulePolicies.newBuilder().setPauseOnFailure(true).build(),
        SchedulePolicies.newBuilder().setPauseOnFailure(false).build());
  }

  @Test
  public void schedulePolicies_toString() {
    String s =
        SchedulePolicies.newBuilder()
            .setOverlapPolicy(ScheduleOverlapPolicy.CANCEL_PREVIOUS)
            .setPauseOnFailure(true)
            .build()
            .toString();
    assertTrue(s.contains("CANCEL_PREVIOUS"));
    assertTrue(s.contains("pauseOnFailure=true"));
  }

  @Test
  public void scheduleState_getters() {
    Instant pausedAt = Instant.ofEpochSecond(500);
    ScheduleState state = new ScheduleState(true, "maintenance", pausedAt, "ops-team");

    assertTrue(state.isPaused());
    assertEquals("maintenance", state.getPauseReason());
    assertEquals(pausedAt, state.getPausedAt());
    assertEquals("ops-team", state.getPausedBy());
  }

  @Test
  public void scheduleState_equals() {
    Instant t = Instant.EPOCH;
    ScheduleState a = new ScheduleState(true, "reason", t, "user");
    ScheduleState b = new ScheduleState(true, "reason", t, "user");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void scheduleState_notEqualOnDifferentPaused() {
    assertNotEquals(
        new ScheduleState(true, null, null, null), new ScheduleState(false, null, null, null));
  }

  @Test
  public void scheduleState_toString() {
    String s = new ScheduleState(true, "pause-reason", null, "alice").toString();
    assertTrue(s.contains("paused=true"));
    assertTrue(s.contains("pause-reason"));
  }

  @Test
  public void scheduleInfo_getters() {
    Instant now = Instant.now();
    ScheduleInfo.BackfillInfo bf = new ScheduleInfo.BackfillInfo("bf1", now, now, 3, 10);
    ScheduleInfo info = new ScheduleInfo(now, now, 42L, now, now, Arrays.asList(bf), 5L, 7L);

    assertEquals(now, info.getLastRunTime());
    assertEquals(now, info.getNextRunTime());
    assertEquals(42L, info.getTotalRuns());
    assertEquals(now, info.getCreateTime());
    assertEquals(now, info.getLastUpdateTime());
    assertEquals(1, info.getOngoingBackfills().size());
    assertEquals(5L, info.getMissedRuns());
    assertEquals(7L, info.getSkippedRuns());
  }

  @Test
  public void scheduleInfo_equals() {
    Instant now = Instant.now();
    ScheduleInfo a = new ScheduleInfo(now, now, 5L, now, now, Collections.emptyList(), 1L, 2L);
    ScheduleInfo b = new ScheduleInfo(now, now, 5L, now, now, Collections.emptyList(), 1L, 2L);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void scheduleInfo_notEqualOnDifferentTotalRuns() {
    Instant now = Instant.now();
    assertNotEquals(
        new ScheduleInfo(now, now, 5L, now, now, null, 0L, 0L),
        new ScheduleInfo(now, now, 6L, now, now, null, 0L, 0L));
  }

  @Test
  public void scheduleInfo_nullOngoingBackfillsNormalizesToEmpty() {
    Instant now = Instant.now();
    ScheduleInfo info = new ScheduleInfo(now, now, 0L, now, now, null, 0L, 0L);
    assertNotNull(info.getOngoingBackfills());
    assertTrue(info.getOngoingBackfills().isEmpty());
  }

  @Test
  public void scheduleInfo_toString() {
    Instant now = Instant.ofEpochSecond(1000);
    String s = new ScheduleInfo(now, now, 3L, now, now, Collections.emptyList(), 1L, 2L).toString();
    assertTrue(s.contains("totalRuns=3"));
    assertTrue(s.contains("missedRuns=1"));
    assertTrue(s.contains("skippedRuns=2"));
    assertTrue(s.contains("lastUpdateTime"));
    assertTrue(s.contains("ongoingBackfills"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void scheduleInfo_ongoingBackfillsIsUnmodifiable() {
    Instant now = Instant.now();
    ScheduleInfo.BackfillInfo bf = new ScheduleInfo.BackfillInfo("id", now, now, 0, 1);
    new ScheduleInfo(now, now, 0L, now, now, Arrays.asList(bf), 0L, 0L)
        .getOngoingBackfills()
        .add(bf);
  }

  @Test
  public void backfillInfo_getters() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    ScheduleInfo.BackfillInfo bf = new ScheduleInfo.BackfillInfo("bf1", start, end, 3, 10);

    assertEquals("bf1", bf.getBackfillId());
    assertEquals(start, bf.getStartTime());
    assertEquals(end, bf.getEndTime());
    assertEquals(3, bf.getRunsCompleted());
    assertEquals(10, bf.getRunsTotal());
  }

  @Test
  public void backfillInfo_equals() {
    Instant t = Instant.EPOCH;
    ScheduleInfo.BackfillInfo a = new ScheduleInfo.BackfillInfo("id", t, t, 1, 5);
    ScheduleInfo.BackfillInfo b = new ScheduleInfo.BackfillInfo("id", t, t, 1, 5);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void backfillInfo_notEqualOnDifferentRunsCompleted() {
    Instant t = Instant.EPOCH;
    assertNotEquals(
        new ScheduleInfo.BackfillInfo("id", t, t, 1, 5),
        new ScheduleInfo.BackfillInfo("id", t, t, 2, 5));
  }

  @Test
  public void backfillInfo_toString() {
    Instant t = Instant.EPOCH;
    String s = new ScheduleInfo.BackfillInfo("myId", t, t, 2, 8).toString();
    assertTrue(s.contains("myId"));
    assertTrue(s.contains("runsCompleted=2"));
    assertTrue(s.contains("runsTotal=8"));
  }

  @Test
  public void scheduleDescription_getters() {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder().setStartWorkflow(minimalStartWorkflowAction().build()).build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    Instant now = Instant.now();
    ScheduleInfo info = new ScheduleInfo(now, now, 0L, now, now, null, 0L, 0L);
    Map<String, Object> memo = Collections.singletonMap("k", "v");
    Map<String, Object> sa = Collections.singletonMap("sk", "sv");

    ScheduleDescription desc =
        new ScheduleDescription(spec, action, policies, state, info, memo, sa);

    assertEquals(spec, desc.getSpec());
    assertEquals(action, desc.getAction());
    assertEquals(policies, desc.getPolicies());
    assertEquals(state, desc.getState());
    assertEquals(info, desc.getInfo());
    assertEquals(memo, desc.getMemo());
    assertEquals(sa, desc.getSearchAttributes());
  }

  @Test
  public void scheduleDescription_equals() {
    assertEquals(minimalDescription(), minimalDescription());
    assertEquals(minimalDescription().hashCode(), minimalDescription().hashCode());
  }

  @Test
  public void scheduleDescription_notEqualOnDifferentSpec() {
    ScheduleAction action =
        ScheduleAction.newBuilder().setStartWorkflow(minimalStartWorkflowAction().build()).build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info =
        new ScheduleInfo(
            Instant.EPOCH, Instant.EPOCH, 0L, Instant.EPOCH, Instant.EPOCH, null, 0L, 0L);
    assertNotEquals(
        new ScheduleDescription(
            ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build(),
            action,
            policies,
            state,
            info,
            null,
            null),
        new ScheduleDescription(
            ScheduleSpec.newBuilder().setCronExpression("0 9 * * *").build(),
            action,
            policies,
            state,
            info,
            null,
            null));
  }

  @Test
  public void scheduleDescription_toString() {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder().setStartWorkflow(minimalStartWorkflowAction().build()).build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info =
        new ScheduleInfo(
            Instant.EPOCH, Instant.EPOCH, 0L, Instant.EPOCH, Instant.EPOCH, null, 0L, 0L);
    ScheduleDescription desc =
        new ScheduleDescription(
            spec, action, policies, state, info, Collections.singletonMap("mk", "mv"), null);
    String s = desc.toString();
    assertTrue(s.contains("spec="));
    assertTrue(s.contains("action="));
    assertTrue(s.contains("policies="));
    assertTrue(s.contains("state="));
    assertTrue(s.contains("info="));
    assertTrue(s.contains("memo="));
    assertTrue(s.contains("searchAttributes="));
  }

  @Test
  public void scheduleDescription_nullMemoNormalizesToEmpty() {
    assertNotNull(minimalDescription().getMemo());
    assertTrue(minimalDescription().getMemo().isEmpty());
    assertNotNull(minimalDescription().getSearchAttributes());
    assertTrue(minimalDescription().getSearchAttributes().isEmpty());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void scheduleDescription_memoIsUnmodifiable() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("k", "v");
    ScheduleDescription desc =
        new ScheduleDescription(
            ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(minimalStartWorkflowAction().build())
                .build(),
            SchedulePolicies.newBuilder().build(),
            new ScheduleState(false, null, null, null),
            new ScheduleInfo(
                Instant.EPOCH, Instant.EPOCH, 0L, Instant.EPOCH, Instant.EPOCH, null, 0L, 0L),
            memo,
            null);
    desc.getMemo().put("extra", "boom");
  }

  @Test
  public void scheduleDescription_memoNotAliasedOnConstruct() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("k", "v");
    ScheduleDescription desc =
        new ScheduleDescription(
            ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build(),
            ScheduleAction.newBuilder()
                .setStartWorkflow(minimalStartWorkflowAction().build())
                .build(),
            SchedulePolicies.newBuilder().build(),
            new ScheduleState(false, null, null, null),
            new ScheduleInfo(
                Instant.EPOCH, Instant.EPOCH, 0L, Instant.EPOCH, Instant.EPOCH, null, 0L, 0L),
            memo,
            null);
    memo.put("extra", "boom");
    assertEquals(1, desc.getMemo().size());
  }

  @Test
  public void scheduleBackfill_twoArgGetters() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    ScheduleBackfill bf = new ScheduleBackfill(start, end);

    assertEquals(start, bf.getStartTime());
    assertEquals(end, bf.getEndTime());
    assertNull(bf.getOverlapPolicy());
  }

  @Test
  public void scheduleBackfill_threeArgGetters() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    ScheduleBackfill bf = new ScheduleBackfill(start, end, ScheduleOverlapPolicy.BUFFER);

    assertEquals(start, bf.getStartTime());
    assertEquals(end, bf.getEndTime());
    assertEquals(ScheduleOverlapPolicy.BUFFER, bf.getOverlapPolicy());
  }

  @Test
  public void scheduleBackfill_equals() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    ScheduleBackfill a = new ScheduleBackfill(start, end);
    ScheduleBackfill b = new ScheduleBackfill(start, end);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void scheduleBackfill_equalsWithPolicy() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    ScheduleBackfill a = new ScheduleBackfill(start, end, ScheduleOverlapPolicy.SKIP_NEW);
    ScheduleBackfill b = new ScheduleBackfill(start, end, ScheduleOverlapPolicy.SKIP_NEW);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void scheduleBackfill_notEqualOnDifferentPolicy() {
    Instant start = Instant.ofEpochSecond(100);
    Instant end = Instant.ofEpochSecond(200);
    assertNotEquals(
        new ScheduleBackfill(start, end),
        new ScheduleBackfill(start, end, ScheduleOverlapPolicy.SKIP_NEW));
  }

  @Test
  public void scheduleBackfill_notEqualOnDifferentStart() {
    Instant end = Instant.ofEpochSecond(200);
    assertNotEquals(
        new ScheduleBackfill(Instant.ofEpochSecond(100), end),
        new ScheduleBackfill(Instant.ofEpochSecond(101), end));
  }

  @Test
  public void scheduleBackfill_toString() {
    String s =
        new ScheduleBackfill(
                Instant.ofEpochSecond(100),
                Instant.ofEpochSecond(200),
                ScheduleOverlapPolicy.BUFFER)
            .toString();
    assertTrue(s.contains("startTime="));
    assertTrue(s.contains("endTime="));
    assertTrue(s.contains("BUFFER"));
  }

  private static ScheduleAction.StartWorkflowAction.Builder minimalStartWorkflowAction() {
    return ScheduleAction.StartWorkflowAction.newBuilder()
        .setWorkflowType("MyWorkflow")
        .setTaskList("my-task-list");
  }

  private static ScheduleDescription minimalDescription() {
    ScheduleSpec spec = ScheduleSpec.newBuilder().setCronExpression("0 * * * *").build();
    ScheduleAction action =
        ScheduleAction.newBuilder().setStartWorkflow(minimalStartWorkflowAction().build()).build();
    SchedulePolicies policies = SchedulePolicies.newBuilder().build();
    ScheduleState state = new ScheduleState(false, null, null, null);
    ScheduleInfo info =
        new ScheduleInfo(
            Instant.EPOCH, Instant.EPOCH, 0L, Instant.EPOCH, Instant.EPOCH, null, 0L, 0L);
    return new ScheduleDescription(spec, action, policies, state, info, null, null);
  }
}
