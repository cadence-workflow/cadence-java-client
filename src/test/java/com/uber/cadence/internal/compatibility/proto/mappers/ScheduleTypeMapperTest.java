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
package com.uber.cadence.internal.compatibility.proto.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.uber.cadence.api.v1.SchedulePauseInfo;
import com.uber.cadence.api.v1.ScheduleState;
import org.junit.Test;

public class ScheduleTypeMapperTest {

  // --- schedulePauseInfo(thrift→proto) ---

  @Test
  public void schedulePauseInfo_null_returnsDefault() {
    assertEquals(
        SchedulePauseInfo.getDefaultInstance(),
        TypeMapper.schedulePauseInfo((com.uber.cadence.SchedulePauseInfo) null));
  }

  @Test
  public void schedulePauseInfo_setsReasonAndPausedBy() {
    com.uber.cadence.SchedulePauseInfo t = new com.uber.cadence.SchedulePauseInfo();
    t.setReason("deploy");
    t.setPausedBy("ci");

    SchedulePauseInfo proto = TypeMapper.schedulePauseInfo(t);

    assertEquals("deploy", proto.getReason());
    assertEquals("ci", proto.getPausedBy());
  }

  // --- scheduleState(thrift→proto) ---

  @Test
  public void scheduleState_null_returnsDefault() {
    assertEquals(
        ScheduleState.getDefaultInstance(),
        TypeMapper.scheduleState((com.uber.cadence.ScheduleState) null));
  }

  @Test
  public void scheduleState_pausedNoPauseInfo_noPauseInfoInProto() {
    com.uber.cadence.ScheduleState t = new com.uber.cadence.ScheduleState().setPaused(true);

    ScheduleState proto = TypeMapper.scheduleState(t);

    assertTrue(proto.getPaused());
    assertFalse(proto.hasPauseInfo());
  }

  @Test
  public void scheduleState_withPauseInfo_populatesPauseInfo() {
    com.uber.cadence.SchedulePauseInfo pi = new com.uber.cadence.SchedulePauseInfo();
    pi.setReason("maintenance");
    com.uber.cadence.ScheduleState t =
        new com.uber.cadence.ScheduleState().setPaused(true).setPauseInfo(pi);

    ScheduleState proto = TypeMapper.scheduleState(t);

    assertTrue(proto.getPaused());
    assertEquals("maintenance", proto.getPauseInfo().getReason());
  }
}
