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
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

import com.uber.cadence.CronOverlapPolicy;
import com.uber.cadence.ParentClosePolicy;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowType;
import com.uber.cadence.internal.common.RetryParameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class StartChildWorkflowExecutionParametersTest {

  @Test
  public void testBuilderAndGetters() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy closePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters parameters =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(cronOverlapPolicy)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(closePolicy)
            .build();

    assertEquals(domain, parameters.getDomain());
    assertEquals(control, parameters.getControl());
    assertEquals(executionTimeout, parameters.getExecutionStartToCloseTimeoutSeconds());
    assertEquals(taskList, parameters.getTaskList());
    assertEquals(taskTimeout, parameters.getTaskStartToCloseTimeoutSeconds());
    assertEquals(workflowId, parameters.getWorkflowId());
    assertEquals(workflowType, parameters.getWorkflowType());
    assertEquals(reusePolicy, parameters.getWorkflowIdReusePolicy());
    assertEquals(retryParameters, parameters.getRetryParameters());
    assertEquals(cronSchedule, parameters.getCronSchedule());
    assertEquals(cronOverlapPolicy, parameters.getCronOverlapPolicy());
    assertEquals(memo, parameters.getMemo());
    assertEquals(searchAttributes, parameters.getSearchAttributes());
    assertEquals(context, parameters.getContext());
    assertEquals(closePolicy, parameters.getParentClosePolicy());
    assertEquals(input, parameters.getInput());
  }

  @Test
  public void testEqualsAndHashCode() {
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");

    RetryParameters retryParameters = new RetryParameters();
    retryParameters.setInitialIntervalInSeconds(10);
    retryParameters.setBackoffCoefficient(2.0);
    retryParameters.setMaximumIntervalInSeconds(100);
    retryParameters.setMaximumAttempts(5);

    String cronSchedule = "0 * * * *";

    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("attrKey1", "attrValue1");

    Map<String, byte[]> context = new HashMap<>();
    context.put("contextKey1", new byte[] {4, 5, 6});

    StartChildWorkflowExecutionParameters parameters1 =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain("domain1")
            .setControl("control1")
            .setExecutionStartToCloseTimeoutSeconds(1000L)
            .setInput(new byte[] {1, 2, 3})
            .setTaskList("taskList1")
            .setTaskStartToCloseTimeoutSeconds(2000L)
            .setWorkflowId("workflowId1")
            .setWorkflowType(new WorkflowType().setName("workflowType1"))
            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(ParentClosePolicy.TERMINATE)
            .build();

    StartChildWorkflowExecutionParameters parameters2 =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain("domain1")
            .setControl("control1")
            .setExecutionStartToCloseTimeoutSeconds(1000L)
            .setInput(new byte[] {1, 2, 3})
            .setTaskList("taskList1")
            .setTaskStartToCloseTimeoutSeconds(2000L)
            .setWorkflowId("workflowId1")
            .setWorkflowType(new WorkflowType().setName("workflowType1"))
            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(ParentClosePolicy.TERMINATE)
            .build();

    assertEquals(parameters1, parameters2);
    assertEquals(parameters1.hashCode(), parameters2.hashCode());
  }

  @Test
  public void testToString() {
    StartChildWorkflowExecutionParameters parameters =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain("testDomain")
            .setControl("testControl")
            .setExecutionStartToCloseTimeoutSeconds(1000L)
            .setInput(new byte[] {1, 2, 3})
            .setTaskList("testTaskList")
            .setTaskStartToCloseTimeoutSeconds(2000L)
            .setWorkflowId("testWorkflowId")
            .setWorkflowType(new WorkflowType().setName("testWorkflowType"))
            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
            .setRetryParameters(new RetryParameters())
            .setCronSchedule("* * * * *")
            .setMemo(new HashMap<>())
            .setSearchAttributes(new HashMap<>())
            .setContext(new HashMap<>())
            .setParentClosePolicy(ParentClosePolicy.TERMINATE)
            .build();

    assertNotNull(parameters.toString());
    assertTrue(parameters.toString().contains("testDomain"));
    assertTrue(parameters.toString().contains("testControl"));
    assertTrue(parameters.toString().contains("1000"));
  }

  @Test
  public void testCronOverlapPolicyField() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy parentClosePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters parameters =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(cronOverlapPolicy)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    assertEquals(CronOverlapPolicy.BUFFERONE, parameters.getCronOverlapPolicy());
  }

  @Test
  public void testCronOverlapPolicyInEqualsAndHashCode() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy parentClosePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters params1 =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(CronOverlapPolicy.BUFFERONE)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    StartChildWorkflowExecutionParameters params2 =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(CronOverlapPolicy.BUFFERONE)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    StartChildWorkflowExecutionParameters params3 =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(CronOverlapPolicy.SKIPPED)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    assertEquals(params1, params2);
    assertNotEquals(params1, params3);
    assertEquals(params1.hashCode(), params2.hashCode());
    assertNotEquals(params1.hashCode(), params3.hashCode());
  }

  @Test
  public void testCronOverlapPolicyInToString() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy parentClosePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters parameters =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(cronOverlapPolicy)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    String toString = parameters.toString();
    assertTrue(toString.contains("cronOverlapPolicy=" + cronOverlapPolicy));
  }

  @Test
  public void testCronOverlapPolicyInCopy() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy parentClosePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters original =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setCronOverlapPolicy(cronOverlapPolicy)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    StartChildWorkflowExecutionParameters copy = original.copy();
    assertEquals(original.getCronOverlapPolicy(), copy.getCronOverlapPolicy());
    assertEquals(original, copy);
  }

  @Test
  public void testCronOverlapPolicyDefaultValue() {
    String domain = "testDomain";
    String control = "testControl";
    long executionTimeout = 1000L;
    byte[] input = {1, 2, 3};
    String taskList = "testTaskList";
    long taskTimeout = 2000L;
    String workflowId = "testWorkflowId";
    WorkflowType workflowType = new WorkflowType().setName("testWorkflowType");
    WorkflowIdReusePolicy reusePolicy = WorkflowIdReusePolicy.AllowDuplicate;
    RetryParameters retryParameters = new RetryParameters();
    String cronSchedule = "* * * * *";
    CronOverlapPolicy cronOverlapPolicy = CronOverlapPolicy.BUFFERONE;
    Map<String, Object> memo = new HashMap<>();
    memo.put("key1", "value1");
    Map<String, Object> searchAttributes = new HashMap<>();
    searchAttributes.put("key2", "value2");
    Map<String, byte[]> context = new HashMap<>();
    context.put("key3", new byte[] {4, 5, 6});
    ParentClosePolicy parentClosePolicy = ParentClosePolicy.TERMINATE;

    StartChildWorkflowExecutionParameters parameters =
        new StartChildWorkflowExecutionParameters.Builder()
            .setDomain(domain)
            .setControl(control)
            .setExecutionStartToCloseTimeoutSeconds(executionTimeout)
            .setInput(input)
            .setTaskList(taskList)
            .setTaskStartToCloseTimeoutSeconds(taskTimeout)
            .setWorkflowId(workflowId)
            .setWorkflowType(workflowType)
            .setWorkflowIdReusePolicy(reusePolicy)
            .setRetryParameters(retryParameters)
            .setCronSchedule(cronSchedule)
            .setMemo(memo)
            .setSearchAttributes(searchAttributes)
            .setContext(context)
            .setParentClosePolicy(parentClosePolicy)
            .build();

    assertEquals(0, parameters.getCronOverlapPolicy()); // should default to 0
  }
}
