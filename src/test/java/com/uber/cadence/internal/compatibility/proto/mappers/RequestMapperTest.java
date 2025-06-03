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

package com.uber.cadence.internal.compatibility.proto.mappers;

import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertMissingFields;
import static com.uber.cadence.internal.compatibility.MapperTestUtil.assertNoMissingFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.uber.cadence.internal.compatibility.ClientObjects;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RequestMapperTest<T, P extends Message> {

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
            ClientObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.COUNT_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::countWorkflowExecutionsRequest),
        testCase(
            ClientObjects.DESCRIBE_TASK_LIST_REQUEST,
            ProtoObjects.DESCRIBE_TASK_LIST_REQUEST,
            RequestMapper::describeTaskListRequest),
        testCase(
            ClientObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listArchivedWorkflowExecutionsRequest),
        testCase(
            ClientObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::requestCancelWorkflowExecutionRequest,
            "firstExecutionRunID", // optional field
            "cause"), // optional field
        testCase(
            ClientObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST_FULL,
            ProtoObjects.REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST_FULL,
            RequestMapper::requestCancelWorkflowExecutionRequest),
        testCase(
            ClientObjects.RESET_STICKY_TASK_LIST_REQUEST,
            ProtoObjects.RESET_STICKY_TASK_LIST_REQUEST,
            RequestMapper::resetStickyTaskListRequest),
        testCase(
            ClientObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.RESET_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::resetWorkflowExecutionRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskCanceledByIdRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_CANCELED_REQUEST,
            RequestMapper::respondActivityTaskCanceledRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskCompletedByIdRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST,
            RequestMapper::respondActivityTaskCompletedRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST,
            RequestMapper::respondActivityTaskFailedByIdRequest),
        testCase(
            ClientObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST,
            ProtoObjects.RESPOND_ACTIVITY_TASK_FAILED_REQUEST,
            RequestMapper::respondActivityTaskFailedRequest),
        testCase(
            ClientObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_DECISION_TASK_COMPLETED_REQUEST,
            RequestMapper::respondDecisionTaskCompletedRequest,
            "scheduleActivityTaskDecisionAttributes", // all other types are missing as expected
            "requestCancelActivityTaskDecisionAttributes",
            "startTimerDecisionAttributes",
            "failWorkflowExecutionDecisionAttributes",
            "cancelTimerDecisionAttributes",
            "cancelWorkflowExecutionDecisionAttributes",
            "requestCancelExternalWorkflowExecutionDecisionAttributes",
            "recordMarkerDecisionAttributes",
            "continueAsNewWorkflowExecutionDecisionAttributes",
            "startChildWorkflowExecutionDecisionAttributes",
            "signalExternalWorkflowExecutionDecisionAttributes",
            "upsertWorkflowSearchAttributesDecisionAttributes"),
        testCase(
            ClientObjects.RESPOND_DECISION_TASK_FAILED_REQUEST,
            ProtoObjects.RESPOND_DECISION_TASK_FAILED_REQUEST,
            RequestMapper::respondDecisionTaskFailedRequest),
        testCase(
            ClientObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST,
            ProtoObjects.RESPOND_QUERY_TASK_COMPLETED_REQUEST,
            RequestMapper::respondQueryTaskCompletedRequest),
        testCase(
            ClientObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.SCAN_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::scanWorkflowExecutionsRequest),
        testCase(
            ClientObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.DESCRIBE_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::describeWorkflowExecutionRequest),
        testCase(
            ClientObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
            ProtoObjects.GET_WORKFLOW_EXECUTION_HISTORY_REQUEST,
            RequestMapper::getWorkflowExecutionHistoryRequest),
        testCase(
            ClientObjects.START_WORKFLOW_EXECUTION,
            ProtoObjects.START_WORKFLOW_EXECUTION,
            RequestMapper::startWorkflowExecutionRequest),
        testCase(
            ClientObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION,
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION,
            RequestMapper::signalWithStartWorkflowExecutionRequest),
        testCase(
            ClientObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            ProtoObjects.START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            RequestMapper::startWorkflowExecutionAsyncRequest),
        testCase(
            ClientObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            ProtoObjects.SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST,
            RequestMapper::signalWithStartWorkflowExecutionAsyncRequest),
        testCase(
            ClientObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.SIGNAL_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::signalWorkflowExecutionRequest),
        testCase(
            ClientObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST,
            ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST,
            RequestMapper::terminateWorkflowExecutionRequest,
            "firstExecutionRunID"), // optional field
        testCase(
            ClientObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST_FULL,
            ProtoObjects.TERMINATE_WORKFLOW_EXECUTION_REQUEST_FULL,
            RequestMapper::terminateWorkflowExecutionRequest),
        testCase(
            ClientObjects.DEPRECATE_DOMAIN_REQUEST,
            ProtoObjects.DEPRECATE_DOMAIN_REQUEST,
            RequestMapper::deprecateDomainRequest),
        testCase(
            ClientObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
            ProtoObjects.DESCRIBE_DOMAIN_BY_ID_REQUEST,
            RequestMapper::describeDomainRequest,
            "name"), // Not needed for query by ID
        testCase(
            ClientObjects.DESCRIBE_DOMAIN_BY_NAME_REQUEST,
            ProtoObjects.DESCRIBE_DOMAIN_BY_NAME_REQUEST,
            RequestMapper::describeDomainRequest,
            "uuid"), // Not needed for query by name
        testCase(
            ClientObjects.LIST_DOMAINS_REQUEST,
            ProtoObjects.LIST_DOMAINS_REQUEST,
            RequestMapper::listDomainsRequest),
        testCase(
            ClientObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
            ProtoObjects.LIST_TASK_LIST_PARTITIONS_REQUEST,
            RequestMapper::listTaskListPartitionsRequest),
        testCase(
            ClientObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
            ProtoObjects.POLL_FOR_ACTIVITY_TASK_REQUEST,
            RequestMapper::pollForActivityTaskRequest),
        testCase(
            ClientObjects.POLL_FOR_DECISION_TASK_REQUEST,
            ProtoObjects.POLL_FOR_DECISION_TASK_REQUEST,
            RequestMapper::pollForDecisionTaskRequest),
        testCase(
            ClientObjects.QUERY_WORKFLOW_REQUEST,
            ProtoObjects.QUERY_WORKFLOW_REQUEST,
            RequestMapper::queryWorkflowRequest),
        testCase(
            ClientObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST,
            RequestMapper::recordActivityTaskHeartbeatByIdRequest),
        testCase(
            ClientObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
            ProtoObjects.RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST,
            RequestMapper::recordActivityTaskHeartbeatRequest),
        testCase(
            ClientObjects.REGISTER_DOMAIN_REQUEST,
            ProtoObjects.REGISTER_DOMAIN_REQUEST,
            RequestMapper
                ::registerDomainRequest), // Thrift has this field but proto doens't have it
        testCase(
            ClientObjects.UPDATE_DOMAIN_REQUEST,
            // Data and replicationConfiguration are copied incorrectly due to a bug :(
            ProtoObjects.UPDATE_DOMAIN_REQUEST,
            RequestMapper::updateDomainRequest),
        testCase(
            ClientObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listClosedWorkflowExecutionsRequest),
        testCase(
            ClientObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
            ProtoObjects.LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST,
            RequestMapper::listOpenWorkflowExecutionsRequest),
        testCase(
            ClientObjects.LIST_WORKFLOW_EXECUTIONS_REQUEST,
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
