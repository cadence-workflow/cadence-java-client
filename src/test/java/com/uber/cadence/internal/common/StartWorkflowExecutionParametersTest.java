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
package com.uber.cadence.internal.common;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

import com.uber.cadence.WorkflowType;
import org.junit.Before;
import org.junit.Test;

public class StartWorkflowExecutionParametersTest {

  private StartWorkflowExecutionParameters params1;
  private StartWorkflowExecutionParameters params2;
  private StartWorkflowExecutionParameters params3;
  private StartWorkflowExecutionParameters differentParams;

  @Before
  public void setUp() {
    params1 = new StartWorkflowExecutionParameters();
    params2 = new StartWorkflowExecutionParameters();
    params3 = new StartWorkflowExecutionParameters();
    differentParams = new StartWorkflowExecutionParameters();

    params1.setWorkflowId("workflow123");
    params1.setWorkflowType(new WorkflowType().setName("sampleWorkflow"));
    params1.setTaskList("taskList1");
    params1.setInput(new byte[] {1, 2, 3});
    params1.setExecutionStartToCloseTimeoutSeconds(60);
    params1.setTaskStartToCloseTimeoutSeconds(30);
    params1.setRetryParameters(new RetryParameters());
    params1.setCronSchedule("* * * * *");

    params2 = params1;
    params3 = params1.copy();

    // Set differentParams with different values to test inequality
    differentParams.setWorkflowId("workflow456");
    differentParams.setWorkflowType(new WorkflowType().setName("sampleWorkflow2"));
  }

  @Test
  public void testToString() {
    String expectedString =
        "StartWorkflowExecutionParameters{workflowId='workflow123', "
            + "workflowType=WorkflowType(name=sampleWorkflow), taskList='taskList1', "
            + "input=[1, 2, 3], executionStartToCloseTimeoutSeconds=60, "
            + "taskStartToCloseTimeoutSeconds=30, workflowIdReusePolicy=null, "
            + "retryParameters=RetryParameters{initialIntervalInSeconds=0, "
            + "backoffCoefficient=0.0, maximumIntervalInSeconds=0, "
            + "maximumAttempts=0, "
            + "nonRetriableErrorReasons=null, expirationIntervalInSeconds=0}, "
            + "cronSchedule='* * * * *', "
            + "memo='null', searchAttributes='null, context='null, delayStart='null'}";

    assertEquals(expectedString, params1.toString());
  }

  @Test
  public void testEquals_sameValues() {
    assertTrue(params1.equals(params2));
    assertTrue(params2.equals(params1));
  }

  @Test
  public void testEquals_differentValues() {
    assertFalse(params1.equals(differentParams));
    assertFalse(params2.equals(differentParams));
  }

  @Test
  public void testHashCode() {
    assertEquals(params1.hashCode(), params2.hashCode());
    assertNotEquals(params1.hashCode(), differentParams.hashCode());
  }

  @Test
  public void testEquals_nullAndDifferentClass() {
    assertFalse(params1.equals(null));
    assertFalse(params1.equals("Some String"));
  }

  @Test
  public void testCopy() {
    StartWorkflowExecutionParameters copy = params1.copy();

    // Verify that all properties are copied correctly
    assertEquals(params1.getWorkflowId(), copy.getWorkflowId());
    assertEquals(params1.getWorkflowType(), copy.getWorkflowType());
    assertEquals(params1.getTaskList(), copy.getTaskList());
    assertArrayEquals(params1.getInput(), copy.getInput());
    assertEquals(
        params1.getExecutionStartToCloseTimeoutSeconds(),
        copy.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(
        params1.getTaskStartToCloseTimeoutSeconds(), copy.getTaskStartToCloseTimeoutSeconds());
    assertEquals(
        params1.getRetryParameters().getInitialIntervalInSeconds(),
        copy.getRetryParameters().getInitialIntervalInSeconds());
    assertEquals(
        params1.getRetryParameters().getBackoffCoefficient(),
        copy.getRetryParameters().getBackoffCoefficient(),
        0.0);
    assertEquals(
        params1.getRetryParameters().getMaximumIntervalInSeconds(),
        copy.getRetryParameters().getMaximumIntervalInSeconds());
    assertEquals(
        params1.getRetryParameters().getMaximumAttempts(),
        copy.getRetryParameters().getMaximumAttempts());
    assertEquals(
        params1.getRetryParameters().getExpirationIntervalInSeconds(),
        copy.getRetryParameters().getExpirationIntervalInSeconds());
    assertEquals(params1.getCronSchedule(), copy.getCronSchedule());
  }
}
