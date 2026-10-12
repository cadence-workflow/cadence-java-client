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

package com.uber.cadence.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorkerTest {

  @Test
  public void testStickyTaskListName_bothUnset_fallsBackToGeneratedIdentityAndUuid() {
    String stickyTaskList = Worker.getStickyTaskListName("some-identity", null);
    assertTrue(stickyTaskList.startsWith("some-identity:"));
    assertNotEquals("some-identity", stickyTaskList);
  }

  @Test
  public void
      testStickyTaskListName_identitySetUniqueIdentityUnset_usesIdentityPlusGeneratedUuid() {
    String stickyTaskList = Worker.getStickyTaskListName("custom-identity", null);
    assertTrue(stickyTaskList.startsWith("custom-identity:"));
  }

  @Test
  public void testStickyTaskListName_uniqueIdentitySet_usedVerbatim() {
    String stickyTaskList = Worker.getStickyTaskListName("some-identity", "my-unique-identity");
    assertEquals("my-unique-identity", stickyTaskList);
  }

  @Test
  public void testStickyTaskListName_bothSet_uniqueIdentityStillWinsVerbatim() {
    String stickyTaskList = Worker.getStickyTaskListName("custom-identity", "my-unique-identity");
    assertEquals("my-unique-identity", stickyTaskList);
  }

  @Test
  public void testWorkerOptions_identityAndUniqueIdentity_defaultToNull() {
    WorkerOptions options = WorkerOptions.newBuilder().build();
    assertEquals(null, options.getIdentity());
    assertEquals(null, options.getUniqueIdentity());
  }

  @Test
  public void testWorkerOptions_identityAndUniqueIdentity_roundTripThroughBuilder() {
    WorkerOptions options =
        WorkerOptions.newBuilder()
            .setIdentity("custom-identity")
            .setUniqueIdentity("custom-unique-identity")
            .build();
    assertEquals("custom-identity", options.getIdentity());
    assertEquals("custom-unique-identity", options.getUniqueIdentity());

    // newBuilder(options) should preserve both fields.
    WorkerOptions copy = WorkerOptions.newBuilder(options).build();
    assertEquals("custom-identity", copy.getIdentity());
    assertEquals("custom-unique-identity", copy.getUniqueIdentity());
  }
}
