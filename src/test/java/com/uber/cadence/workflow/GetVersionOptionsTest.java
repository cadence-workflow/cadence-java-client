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

package com.uber.cadence.workflow;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Test;

public class GetVersionOptionsTest {

  @Test
  public void testExecuteWithVersion() {
    GetVersionOptions options = GetVersionOptions.newBuilder().executeWithVersion(5).build();

    assertEquals(Optional.of(5), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testExecuteWithMinVersion() {
    GetVersionOptions options = GetVersionOptions.newBuilder().executeWithMinVersion().build();

    assertEquals(Optional.empty(), options.getCustomVersion());
    assertTrue(options.isUseMinVersion());
  }

  @Test
  public void testDefaultOptions() {
    GetVersionOptions options = GetVersionOptions.newBuilder().build();

    assertEquals(Optional.empty(), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testBuilderChaining() {
    GetVersionOptions options =
        GetVersionOptions.newBuilder().executeWithVersion(3).executeWithMinVersion().build();

    // When both are set, custom version takes precedence
    assertEquals(Optional.of(3), options.getCustomVersion());
    assertTrue(options.isUseMinVersion());
  }

  @Test
  public void testMultipleExecuteWithVersionCalls() {
    GetVersionOptions options =
        GetVersionOptions.newBuilder().executeWithVersion(1).executeWithVersion(2).build();

    // Last call should take precedence
    assertEquals(Optional.of(2), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testZeroVersion() {
    GetVersionOptions options = GetVersionOptions.newBuilder().executeWithVersion(0).build();

    assertEquals(Optional.of(0), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }

  @Test
  public void testNegativeVersion() {
    GetVersionOptions options = GetVersionOptions.newBuilder().executeWithVersion(-1).build();

    assertEquals(Optional.of(-1), options.getCustomVersion());
    assertFalse(options.isUseMinVersion());
  }
}
