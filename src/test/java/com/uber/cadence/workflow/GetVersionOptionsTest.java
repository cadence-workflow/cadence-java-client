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
package com.uber.cadence.workflow;

import static org.junit.Assert.*;

import org.junit.Test;

public class GetVersionOptionsTest {

  @Test
  public void testDefaultBuild() {
    GetVersionOptions options = new GetVersionOptions.Builder().build();
    assertNull(options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testExecuteWithVersionFactory() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(2);
    assertEquals(Integer.valueOf(2), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testExecuteWithMinVersionFactory() {
    GetVersionOptions options = GetVersionOptions.executeWithMinVersion();
    assertNull(options.getCustomVersion());
    assertTrue(options.isUseMinVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMutualExclusionThrows() {
    new GetVersionOptions.Builder().setCustomVersion(2).setUseMinVersion(true).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomVersionCannotBeDefaultVersion() {
    GetVersionOptions.executeWithVersion(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomVersionCannotBeNegative() {
    GetVersionOptions.executeWithVersion(-2);
  }

  @Test
  public void testToString() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(3);
    assertEquals("GetVersionOptions{customVersion=3, useMinVersion=false}", options.toString());
  }

  @Test
  public void testExecuteWithVersionZero() {
    GetVersionOptions options = GetVersionOptions.executeWithVersion(0);
    assertEquals(Integer.valueOf(0), options.getCustomVersion());
  }
}
