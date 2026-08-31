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

package com.uber.cadence.internal.compatibility.thrift;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertMissingFields;
import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import com.uber.cadence.internal.compatibility.ThriftObjects;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ResponseMapperTest<
    F extends Enum<F> & TFieldIdEnum, T extends TBase<T, F>, P extends Message> {

  @Parameterized.Parameter(0)
  public String testName;

  @Parameterized.Parameter(1)
  public P from;

  @Parameterized.Parameter(2)
  public T to;

  @Parameterized.Parameter(3)
  public Function<P, T> via;

  @Parameterized.Parameter(4)
  public Set<String> missingFields;

  @Test
  public void testFieldsPresent() {
    // If IDL is updated, this will fail. Update the mapper or add it to the test
    if (missingFields.isEmpty()) {
      assertNoMissingFields(to);
    } else {
      assertMissingFields(to, missingFields);
    }
  }

  @Test
  public void testMapper() {
    T actual = via.apply(from);
    assertEquals(to, actual);
  }

  @Test
  public void testHandlesNull() {
    T actual = via.apply(null);

    assertNull("Mapper functions should accept null, returning null", actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(
        testCase(
            ProtoObjects.START_WORKFLOW_EXECUTION_RESPONSE,
            ThriftObjects.START_WORKFLOW_EXECUTION_RESPONSE,
            ResponseMapper::startWorkflowExecutionResponse),
        testCase(
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
            ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
            ResponseMapper::startWorkflowExecutionAsyncResponse),
        testCase(
            ProtoObjects.DESCRIBE_TASK_LIST_RESPONSE,
            ThriftObjects.DESCRIBE_TASK_LIST_RESPONSE,
            ResponseMapper::describeTaskListResponse,
            "taskList"), // new IDL field on response, not wired yet
        testCase(
            ProtoObjects.DESCRIBE_WORKFLOW_EXECUTION_RESPONSE,
            ThriftObjects.DESCRIBE_WORKFLOW_EXECUTION_RESPONSE,
            ResponseMapper::describeWorkflowExecutionResponse),
        testCase(
            ProtoObjects.GET_SEARCH_ATTRIBUTES_RESPONSE,
            ThriftObjects.GET_SEARCH_ATTRIBUTES_RESPONSE,
            ResponseMapper::getSearchAttributesResponse),
        testCase(
            ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE,
            ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE,
            ResponseMapper::getWorkflowExecutionHistoryResponse),
        testCase(
            ProtoObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::listArchivedWorkflowExecutionsResponse),
        testCase(
            ProtoObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::listClosedWorkflowExecutionsResponse),
        testCase(
            ProtoObjects.LIST_TASK_LIST_PARTITIONS_RESPONSE,
            ThriftObjects.LIST_TASK_LIST_PARTITIONS_RESPONSE,
            ResponseMapper::listTaskListPartitionsResponse),
        testCase(
            ProtoObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::listWorkflowExecutionsResponse),
        testCase(
            ProtoObjects.POLL_FOR_ACTIVITY_TASK_RESPONSE,
            ThriftObjects.POLL_FOR_ACTIVITY_TASK_RESPONSE,
            ResponseMapper::pollForActivityTaskResponse),
        testCase(
            ProtoObjects.POLL_FOR_DECISION_TASK_RESPONSE,
            ThriftObjects.POLL_FOR_DECISION_TASK_RESPONSE,
            ResponseMapper::pollForDecisionTaskResponse,
            "totalHistoryBytes"),
        testCase(
            ProtoObjects.QUERY_WORKFLOW_RESPONSE,
            ThriftObjects.QUERY_WORKFLOW_RESPONSE,
            ResponseMapper::queryWorkflowResponse),
        testCase(
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE,
            ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE,
            ResponseMapper::recordActivityTaskHeartbeatResponse),
        testCase(
            ProtoObjects.RESET_WORKFLOW_EXECUTION_RESPONSE,
            ThriftObjects.RESET_WORKFLOW_EXECUTION_RESPONSE,
            ResponseMapper::resetWorkflowExecutionResponse),
        testCase(
            ProtoObjects.RESPOND_DECISION_TASK_COMPLETED_RESPONSE,
            ThriftObjects.RESPOND_DECISION_TASK_COMPLETED_RESPONSE,
            ResponseMapper::respondDecisionTaskCompletedResponse),
        testCase(
            ProtoObjects.COUNT_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.COUNT_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::countWorkflowExecutionsResponse),
        testCase(
            ProtoObjects.DESCRIBE_DOMAIN_RESPONSE,
            ThriftObjects.DESCRIBE_DOMAIN_RESPONSE,
            ResponseMapper::describeDomainResponse,
            "failoverInfo"),
        testCase(
            ProtoObjects.LIST_DOMAINS_RESPONSE,
            ThriftObjects.LIST_DOMAINS_RESPONSE,
            ResponseMapper::listDomainsResponse),
        testCase(
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
            ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE,
            ResponseMapper::signalWithStartWorkflowExecutionAsyncResponse),
        testCase(
            ProtoObjects.UPDATE_DOMAIN_RESPONSE,
            ThriftObjects.UPDATE_DOMAIN_RESPONSE,
            ResponseMapper::updateDomainResponse),
        testCase(
            ProtoObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::listOpenWorkflowExecutionsResponse),
        // Proto has more types than thrift because it doesn't reuse response types across methods
        testCase(
            ProtoObjects.SCAN_WORKFLOW_EXECUTIONS_RESPONSE,
            ThriftObjects.LIST_WORKFLOW_EXECUTIONS_RESPONSE,
            ResponseMapper::scanWorkflowExecutionsResponse),
        testCase(
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_RESPONSE,
            ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE,
            ResponseMapper::recordActivityTaskHeartbeatByIdResponse),
        testCase(
            ProtoObjects.GET_CLUSTER_INFO_RESPONSE,
            ThriftObjects.CLUSTER_INFO,
            ResponseMapper::getClusterInfoResponse),
        testCase(
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_RESPONSE,
            ThriftObjects.START_WORKFLOW_EXECUTION_RESPONSE,
            ResponseMapper::signalWithStartWorkflowExecutionResponse));
  }

  private static <T, P> Object[] testCase(
      P from, T to, Function<P, T> via, String... missingFields) {
    return new Object[] {
      from.getClass().getSimpleName(), from, to, via, ImmutableSet.copyOf(missingFields)
    };
  }
}
