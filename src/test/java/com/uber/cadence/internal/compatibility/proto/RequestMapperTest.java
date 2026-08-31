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

package com.uber.cadence.internal.compatibility.proto;

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
public class RequestMapperTest<
    F extends Enum<F> & TFieldIdEnum, T extends TBase<T, F>, P extends Message> {

  @Parameterized.Parameter(0)
  public String testName;

  @Parameterized.Parameter(1)
  public T from;

  @Parameterized.Parameter(2)
  public P to;

  @Parameterized.Parameter(3)
  public Function<T, P> via;

  @Parameterized.Parameter(4)
  public Set<String> missingFields;

  @Test
  public void testFieldsPresent() {
    // If IDL is updated, this will fail. Update the mapper or add it to the test
    if (missingFields.isEmpty()) {
      assertNoMissingFields(from);
    } else {
      assertMissingFields(from, missingFields);
    }
  }

  @Test
  public void testMapper() {
    P actual = via.apply(from);
    assertEquals(to, actual);
  }

  @Test
  public void testHandlesNull() {
    P actual = via.apply(null);

    assertNull("Mapper functions should accept null, returning null", actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(
        testCase(
            ThriftObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::countWorkflowExecutionsRequest),
        testCase(
            ThriftObjects.DESCRIBE_TASK_LIST_REQUEST,
            ProtoObjects.DESCRIBE_TASK_LIST_REQUEST,
            RequestMapper::describeTaskListRequest),
        testCase(
            ThriftObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listArchivedWorkflowExecutionsRequest),
        testCase(
            ThriftObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::requestCancelWorkflowExecutionRequest,
            "firstExecutionRunID", // optional field
            "cause"), // optional field
        testCase(
            ThriftObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST_FULL,
            ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST_FULL,
            RequestMapper::requestCancelWorkflowExecutionRequest),
        testCase(
            ThriftObjects.RESET_STICKY_TASK_LIST_REQUEST,
            ProtoObjects.RESET_STICKY_TASK_LIST_REQUEST,
            RequestMapper::resetStickyTaskListRequest),
        testCase(
            ThriftObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::resetWorkflowExecutionRequest),
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskCanceledByIdRequest),
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST,
            RequestMapper::respondActivityTaskCanceledRequest),
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskCompletedByIdRequest),
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST,
            RequestMapper::respondActivityTaskCompletedRequest),
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskFailedByIdRequest,
            "heartbeatDetails", // new IDL field, not wired yet
            "failureOptions"), // new IDL field, not wired yet
        testCase(
            ThriftObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST,
            RequestMapper::respondActivityTaskFailedRequest,
            "heartbeatDetails", // new IDL field, not wired yet
            "failureOptions"), // new IDL field, not wired yet
        testCase(
            ThriftObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
            RequestMapper::respondDecisionTaskCompletedRequest),
        testCase(
            ThriftObjects.RESPOND_DECISION_TASK_FAILED_REQUEST,
            ProtoObjects.RESPOND_DECISION_TASK_FAILED_REQUEST,
            RequestMapper::respondDecisionTaskFailedRequest),
        testCase(
            ThriftObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST,
            RequestMapper::respondQueryTaskCompletedRequest),
        testCase(
            ThriftObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.SCAN_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::scanWorkflowExecutionsRequest),
        testCase(
            ThriftObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::describeWorkflowExecutionRequest),
        testCase(
            ThriftObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
            ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
            RequestMapper::getWorkflowExecutionHistoryRequest),
        testCase(
            ThriftObjects.START_WORKFLOW_EXECUTION,
            ProtoObjects.START_WORKFLOW_EXECUTION,
            RequestMapper::startWorkflowExecutionRequest),
        testCase(
            ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION,
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION,
            RequestMapper::signalWithStartWorkflowExecutionRequest),
        testCase(
            ThriftObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            RequestMapper::startWorkflowExecutionAsyncRequest),
        testCase(
            ThriftObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            RequestMapper::signalWithStartWorkflowExecutionAsyncRequest),
        testCase(
            ThriftObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::signalWorkflowExecutionRequest),
        testCase(
            ThriftObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::terminateWorkflowExecutionRequest,
            "firstExecutionRunID"), // optional field
        testCase(
            ThriftObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST_FULL,
            ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST_FULL,
            RequestMapper::terminateWorkflowExecutionRequest),
        testCase(
            ThriftObjects.DEPRECATE_DOMAIN_REQUEST,
            ProtoObjects.DEPRECATE_DOMAIN_REQUEST,
            RequestMapper::deprecateDomainRequest),
        testCase(
            ThriftObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
            ProtoObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
            RequestMapper::describeDomainRequest,
            "name"), // Not needed for query by ID
        testCase(
            ThriftObjects.DESCRIBE_DOMAIN_BY_NAME_REQUEST,
            ProtoObjects.DESCRIBE_DOMAIN_BY_NAME_REQUEST,
            RequestMapper::describeDomainRequest,
            "uuid"), // Not needed for query by name
        testCase(
            ThriftObjects.LIST_DOMAINS_REQUEST,
            ProtoObjects.LIST_DOMAINS_REQUEST,
            RequestMapper::listDomainsRequest),
        testCase(
            ThriftObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
            ProtoObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
            RequestMapper::listTaskListPartitionsRequest),
        testCase(
            ThriftObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
            ProtoObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
            RequestMapper::pollForActivityTaskRequest),
        testCase(
            ThriftObjects.POLL_FOR_DECISION_TASK_REQUEST,
            ProtoObjects.POLL_FOR_DECISION_TASK_REQUEST,
            RequestMapper::pollForDecisionTaskRequest),
        testCase(
            ThriftObjects.QUERY_WORKFLOW_REQUEST,
            ProtoObjects.QUERY_WORKFLOW_REQUEST,
            RequestMapper::queryWorkflowRequest),
        testCase(
            ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
            RequestMapper::recordActivityTaskHeartbeatByIdRequest),
        testCase(
            ThriftObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
            RequestMapper::recordActivityTaskHeartbeatRequest),
        testCase(
            ThriftObjects.REGISTER_DOMAIN_REQUEST,
            ProtoObjects.REGISTER_DOMAIN_REQUEST,
            RequestMapper::registerDomainRequest,
            "emitMetric", // Thrift has this field but proto doens't have it
            "activeClustersByRegion"), // thrift-only field, no proto counterpart
        testCase(
            ThriftObjects.UPDATE_DOMAIN_REQUEST,
            ProtoObjects.UPDATE_DOMAIN_REQUEST,
            RequestMapper::updateDomainRequest),
        testCase(
            ThriftObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listClosedWorkflowExecutionsRequest),
        testCase(
            ThriftObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listOpenWorkflowExecutionsRequest),
        testCase(
            ThriftObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listWorkflowExecutionsRequest));
  }

  private static <T, P> Object[] testCase(
      T from, P to, Function<T, P> via, String... missingFields) {
    return new Object[] {
      from.getClass().getSimpleName(), from, to, via, ImmutableSet.copyOf(missingFields)
    };
  }
}
