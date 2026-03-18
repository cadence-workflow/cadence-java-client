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

package com.uber.cadence.internal.replay;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.workflow.GetVersionOptions;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.junit.Before;
import org.junit.Test;

public class ClockDecisionContextVersionOptionsTest {

  private DecisionsHelper decisions;
  private ReplayDecider replayDecider;
  private ClockDecisionContext context;
  private final DataConverter converter = JsonDataConverter.getInstance();

  @Before
  public void setUp() {
    decisions = mock(DecisionsHelper.class);
    replayDecider = mock(ReplayDecider.class);
    Lock lock = mock(Lock.class);
    Condition condition = mock(Condition.class);

    when(replayDecider.getLock()).thenReturn(lock);
    when(lock.newCondition()).thenReturn(condition);
    when(decisions.getNextDecisionEventId()).thenReturn(1L, 2L, 3L, 4L, 5L);

    context = new ClockDecisionContext(decisions, null, replayDecider, converter);
    context.setReplaying(false);
  }

  @Test
  public void testDefaultBehaviorRecordsMaxSupported() {
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 1, 3, null);
    assertEquals(3, result.getVersion());
    assertTrue(result.shouldUpdateCadenceChangeVersion());
  }

  @Test
  public void testCustomVersionRecordsSpecifiedVersion() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(2);
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 1, 3, options);
    assertEquals(2, result.getVersion());
    assertTrue(result.shouldUpdateCadenceChangeVersion());
  }

  @Test
  public void testMinVersionRecordsMinSupported() {
    GetVersionOptions options = GetVersionOptions.executeWithMinVersion();
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 1, 3, options);
    assertEquals(1, result.getVersion());
    assertTrue(result.shouldUpdateCadenceChangeVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomVersionBelowRangeThrows() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(0);
    context.getVersion("change1", converter, 1, 3, options);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomVersionAboveRangeThrows() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(5);
    context.getVersion("change1", converter, 1, 3, options);
  }

  @Test
  public void testCachedVersionIgnoresOptions() {
    // First call records version 3 (default, no options)
    ClockDecisionContext.GetVersionResult firstResult =
        context.getVersion("change1", converter, 1, 3, null);
    assertEquals(3, firstResult.getVersion());

    // Second call with customVersion=2 should still return 3 (cached)
    GetVersionOptions options = GetVersionOptions.executeWithVersion(2);
    ClockDecisionContext.GetVersionResult secondResult =
        context.getVersion("change1", converter, 1, 3, options);
    assertEquals(3, secondResult.getVersion());
    assertFalse(secondResult.shouldUpdateCadenceChangeVersion());
  }

  @Test
  public void testCachedVersionIgnoresMinVersionOption() {
    // First call records version 3 (default)
    context.getVersion("change1", converter, 1, 3, null);

    // Second call with useMinVersion should still return 3
    GetVersionOptions options = GetVersionOptions.executeWithMinVersion();
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 1, 3, options);
    assertEquals(3, result.getVersion());
    assertFalse(result.shouldUpdateCadenceChangeVersion());
  }

  @Test
  public void testDifferentChangeIdsAreIndependent() {
    // change1 uses custom version 2
    GetVersionOptions options1 = GetVersionOptions.executeWithVersion(2);
    ClockDecisionContext.GetVersionResult result1 =
        context.getVersion("change1", converter, 1, 3, options1);
    assertEquals(2, result1.getVersion());

    // change2 uses min version
    GetVersionOptions options2 = GetVersionOptions.executeWithMinVersion();
    ClockDecisionContext.GetVersionResult result2 =
        context.getVersion("change2", converter, 1, 3, options2);
    assertEquals(1, result2.getVersion());
  }

  @Test
  public void testNullOptionsUsesDefaultBehavior() {
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 0, 5, null);
    assertEquals(5, result.getVersion());
  }

  @Test
  public void testCustomVersionAtRangeBoundaries() {
    // Custom version equals minSupported
    GetVersionOptions optionsMin = GetVersionOptions.executeWithVersion(1);
    ClockDecisionContext.GetVersionResult resultMin =
        context.getVersion("change1", converter, 1, 3, optionsMin);
    assertEquals(1, resultMin.getVersion());

    // Custom version equals maxSupported
    GetVersionOptions optionsMax = GetVersionOptions.executeWithVersion(3);
    ClockDecisionContext.GetVersionResult resultMax =
        context.getVersion("change2", converter, 1, 3, optionsMax);
    assertEquals(3, resultMax.getVersion());
  }

  @Test
  public void testOutOfRangeCustomVersionIgnoredWhenCached() {
    // First call records version 3 (default, maxSupported)
    context.getVersion("change1", converter, 1, 3, null);

    // Second call with customVersion=5 which is outside [1,3].
    // Should NOT throw because a cached version (3) exists and options are ignored.
    GetVersionOptions options = GetVersionOptions.executeWithVersion(5);
    ClockDecisionContext.GetVersionResult result =
        context.getVersion("change1", converter, 1, 3, options);
    assertEquals(3, result.getVersion());
  }
}
