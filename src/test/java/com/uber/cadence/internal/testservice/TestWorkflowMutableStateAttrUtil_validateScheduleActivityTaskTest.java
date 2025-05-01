/*
 *
 *  *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *  *
 *  *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  *  use this file except in compliance with the License. A copy of the License is
 *  *  located at
 *  *
 *  *  http://aws.amazon.com/apache2.0
 *  *
 *  *  or in the "license" file accompanying this file. This file is distributed on
 *  *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  *  express or implied. See the License for the specific language governing
 *  *  permissions and limitations under the License.
 *
 */

package com.uber.cadence.internal.testservice;

import com.google.protobuf.Duration;
import com.uber.cadence.api.v1.ActivityType;
import com.uber.cadence.api.v1.ScheduleActivityTaskDecisionAttributes;
import com.uber.cadence.api.v1.TaskList;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestWorkflowMutableStateAttrUtil_validateScheduleActivityTaskTest extends TestCase {

  private final ScheduleActivityTaskDecisionAttributes attributes;
  private final String errorMessage;

  public TestWorkflowMutableStateAttrUtil_validateScheduleActivityTaskTest(
      String testName, ScheduleActivityTaskDecisionAttributes attributes, String errorMessage) {
    this.attributes = attributes;
    this.errorMessage = errorMessage;
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"valid", createAtt(), null},
          {"null", null, "ScheduleActivityTaskDecisionAttributes is not set on decision."},
          {"task list null", createAtt().clearTaskList(), "TaskList is not set on decision."},
          {
            "task list no name",
            createAtt().setTaskList(TaskList.newBuilder().setName("")),
            "TaskList is not set on decision."
          },
          {
            "activity id null",
            createAtt().setActivityId(null),
            "ActivityId is not set on decision."
          },
          {
            "activity id empty", createAtt().setActivityId(""), "ActivityId is not set on decision."
          },
          {
            "activity type null",
            createAtt().clearActivityType(),
            "ActivityType is not set on decision."
          },
          {
            "activity type name null",
            createAtt().setActivityType(ActivityType.newBuilder()),
            "ActivityType is not set on decision."
          },
          {
            "activity type name empty",
            createAtt().setActivityType(ActivityType.newBuilder().setName("")),
            "ActivityType is not set on decision."
          },
          {
            "start to close <= 0",
            createAtt().setStartToCloseTimeout(Duration.newBuilder().setSeconds(0)),
            "A valid StartToCloseTimeoutSeconds is not set on decision."
          },
          {
            "schedule to start <= 0",
            createAtt().setScheduleToStartTimeout(Duration.newBuilder().setSeconds(0)),
            "A valid ScheduleToStartTimeoutSeconds is not set on decision."
          },
          {
            "schedule to close <= 0",
            createAtt().setScheduleToCloseTimeout(Duration.newBuilder().setSeconds(0)),
            "A valid ScheduleToCloseTimeoutSeconds is not set on decision."
          },
          {
            "heartbeat < 0",
            createAtt().setHeartbeatTimeout(Duration.newBuilder().setSeconds(-1)),
            "Ac valid HeartbeatTimeoutSeconds is not set on decision."
          },
        });
  }

  @Test
  public void testValidateScheduleActivityTask() {
    try {
      TestWorkflowMutableStateAttrUtil.validateScheduleActivityTask(attributes);
      if (errorMessage != null) {
        fail("Expected exception");
      }
    } catch (Exception e) {
      assertEquals(errorMessage, e.getMessage());
    }
  }

  private static ScheduleActivityTaskDecisionAttributes.Builder createAtt() {
    return ScheduleActivityTaskDecisionAttributes.newBuilder()
        .setTaskList(TaskList.newBuilder().setName("testTaskList"))
        .setActivityId("testActivityId")
        .setActivityType(ActivityType.newBuilder().setName("testActivityType"))
        .setStartToCloseTimeout(Duration.newBuilder().setSeconds(12))
        .setScheduleToStartTimeout(Duration.newBuilder().setSeconds(34))
        .setScheduleToCloseTimeout(Duration.newBuilder().setSeconds(45))
        .setHeartbeatTimeout(Duration.newBuilder().setSeconds(78));
  }
}
