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
package com.uber.cadence.internal.replay;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

import com.uber.cadence.ActivityType;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.internal.common.RetryParameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class ExecuteActivityParametersTest {

  private ExecuteActivityParameters parameters;
  private ActivityType activityType;
  private RetryParameters retryParameters;

  @Before
  public void setUp() {
    RetryOptions retryOptions = new RetryOptions.Builder().build();
    parameters = new ExecuteActivityParameters();
    activityType = new ActivityType();
    retryParameters = new RetryParameters(retryOptions);
  }

  @Test
  public void testActivityId() {
    parameters.setActivityId("activity123");
    assertEquals("activity123", parameters.getActivityId());

    parameters.withActivityId("activity456");
    assertEquals("activity456", parameters.getActivityId());
  }

  @Test
  public void testActivityType() {
    parameters.setActivityType(activityType);
    assertEquals(activityType, parameters.getActivityType());

    ActivityType newType = new ActivityType();
    parameters.withActivityType(newType);
    assertEquals(newType, parameters.getActivityType());
  }

  @Test
  public void testInput() {
    byte[] input = {1, 2, 3};
    parameters.setInput(input);
    assertArrayEquals(input, parameters.getInput());

    byte[] newInput = {4, 5, 6};
    parameters.withInput(newInput);
    assertArrayEquals(newInput, parameters.getInput());
  }

  @Test
  public void testTimeouts() {
    parameters.setHeartbeatTimeoutSeconds(100L);
    assertEquals(100L, parameters.getHeartbeatTimeoutSeconds());

    parameters.withHeartbeatTimeoutSeconds(200L);
    assertEquals(200L, parameters.getHeartbeatTimeoutSeconds());

    parameters.setScheduleToStartTimeoutSeconds(300L);
    assertEquals(300L, parameters.getScheduleToStartTimeoutSeconds());

    parameters.withScheduleToStartTimeoutSeconds(400L);
    assertEquals(400L, parameters.getScheduleToStartTimeoutSeconds());

    parameters.setScheduleToCloseTimeoutSeconds(500L);
    assertEquals(500L, parameters.getScheduleToCloseTimeoutSeconds());

    parameters.withScheduleToCloseTimeoutSeconds(600L);
    assertEquals(600L, parameters.getScheduleToCloseTimeoutSeconds());

    parameters.setStartToCloseTimeoutSeconds(700L);
    assertEquals(700L, parameters.getStartToCloseTimeoutSeconds());

    parameters.withStartToCloseTimeoutSeconds(800L);
    assertEquals(800L, parameters.getStartToCloseTimeoutSeconds());
  }

  @Test
  public void testTaskList() {
    parameters.setTaskList("sampleTaskList");
    assertEquals("sampleTaskList", parameters.getTaskList());

    parameters.withTaskList("newTaskList");
    assertEquals("newTaskList", parameters.getTaskList());
  }

  @Test
  public void testRetryParameters() {
    parameters.setRetryParameters(retryParameters);
    assertEquals(retryParameters, parameters.getRetryParameters());

    RetryParameters newRetryParameters = new RetryParameters();
    parameters.withRetryParameters(newRetryParameters);
    assertEquals(newRetryParameters, parameters.getRetryParameters());
  }

  @Test
  public void testContext() {
    Map<String, byte[]> context = new HashMap<>();
    context.put("key1", new byte[] {1});
    parameters.setContext(context);
    assertEquals(context, parameters.getContext());

    Map<String, byte[]> newContext = new HashMap<>();
    newContext.put("key2", new byte[] {2});
    parameters.withContext(newContext);
    assertEquals(newContext, parameters.getContext());
  }

  @Test
  public void testCopy() {
    parameters.setActivityId("copyTest");
    parameters.setActivityType(activityType);
    parameters.setInput(new byte[] {10, 20});
    parameters.setHeartbeatTimeoutSeconds(900L);
    parameters.setRetryParameters(retryParameters);

    ExecuteActivityParameters copy = parameters.copy();

    assertEquals(parameters.getActivityId(), copy.getActivityId());
    assertEquals(parameters.getActivityType(), copy.getActivityType());
    assertArrayEquals(parameters.getInput(), copy.getInput());
    assertEquals(parameters.getHeartbeatTimeoutSeconds(), copy.getHeartbeatTimeoutSeconds());
    assertEquals(
        parameters.getRetryParameters().initialIntervalInSeconds,
        copy.getRetryParameters().initialIntervalInSeconds);
    assertEquals(
        parameters.getRetryParameters().backoffCoefficient,
        copy.getRetryParameters().backoffCoefficient);
    assertEquals(
        parameters.getRetryParameters().maximumAttempts, copy.getRetryParameters().maximumAttempts);
  }

  @Test
  public void testToString() {
    parameters.setActivityId("toStringTest");
    parameters.setActivityType(activityType);
    parameters.setInput(new byte[] {10, 20});
    String expectedString =
        "ExecuteActivityParameters{activityId='toStringTest', activityType=ActivityType(name=null), heartbeatTimeoutSeconds=0, input=[10, 20], scheduleToCloseTimeoutSeconds=0, scheduleToStartTimeoutSeconds=0, startToCloseTimeoutSeconds=0, taskList='null', retryParameters=null, context='null}";

    assertEquals(expectedString, parameters.toString());
  }
}
