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

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.uber.cadence.entities.WorkflowExecutionCloseStatus;
import com.uber.cadence.internal.compatibility.ClientObjects;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TypeMapperTest<T, P> {

  @Parameterized.Parameter(0)
  public String testName;

  @Parameterized.Parameter(1)
  public T from;

  @Parameterized.Parameter(2)
  public P to;

  @Parameterized.Parameter(3)
  public Function<T, P> via;

  @Test
  public void testMapper() {
    P actual = via.apply(from);
    assertEquals(to, actual);
  }

  @Test
  public void testHandlesNull() {
    P actual = via.apply(null);

    if (actual instanceof List<?>) {
      assertTrue(
          "Mapper functions returning a list should return an empty list",
          ((List<?>) actual).isEmpty());
    } else if (actual instanceof Map<?, ?>) {
      assertTrue(
          "Mapper functions returning a map should return an empty map",
          ((Map<?, ?>) actual).isEmpty());
    } else if (actual instanceof Message) {
      assertEquals(
          "Mapper functions returning a Message should return the default value",
          ((Message) actual).getDefaultInstanceForType(),
          actual);
    } else {
      assertNull("Mapper functions should accept null, returning null", actual);
    }
  }

  @Parameterized.Parameters(name = "{0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(
        testCase(
            ClientObjects.BAD_BINARY_INFO, ProtoObjects.BAD_BINARY_INFO, TypeMapper::badBinaryInfo),
        testCase(
            ClientObjects.utf8Bytes("data"), ProtoObjects.payload("data"), TypeMapper::payload),
        testCase(ClientObjects.ACTIVITY_TYPE, ProtoObjects.ACTIVITY_TYPE, TypeMapper::activityType),
        testCase(ClientObjects.WORKFLOW_TYPE, ProtoObjects.WORKFLOW_TYPE, TypeMapper::workflowType),
        testCase(ClientObjects.TASK_LIST, ProtoObjects.TASK_LIST, TypeMapper::taskList),
        testCase(
            ClientObjects.TASK_LIST_METADATA,
            ProtoObjects.TASK_LIST_METADATA,
            TypeMapper::taskListMetadata),
        testCase(ClientObjects.RETRY_POLICY, ProtoObjects.RETRY_POLICY, TypeMapper::retryPolicy),
        testCase(ClientObjects.HEADER, ProtoObjects.HEADER, TypeMapper::header),
        testCase(ClientObjects.MEMO, ProtoObjects.MEMO, TypeMapper::memo),
        testCase(
            ClientObjects.SEARCH_ATTRIBUTES,
            ProtoObjects.SEARCH_ATTRIBUTES,
            TypeMapper::searchAttributes),
        testCase(ClientObjects.BAD_BINARIES, ProtoObjects.BAD_BINARIES, TypeMapper::badBinaries),
        testCase(
            ClientObjects.CLUSTER_REPLICATION_CONFIGURATION,
            ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION,
            TypeMapper::clusterReplicationConfiguration),
        testCase(
            ClientObjects.WORKFLOW_QUERY, ProtoObjects.WORKFLOW_QUERY, TypeMapper::workflowQuery),
        testCase(
            ClientObjects.WORKFLOW_QUERY_RESULT,
            ProtoObjects.WORKFLOW_QUERY_RESULT,
            TypeMapper::workflowQueryResult),
        testCase(
            ClientObjects.STICKY_EXECUTION_ATTRIBUTES,
            ProtoObjects.STICKY_EXECUTION_ATTRIBUTES,
            TypeMapper::stickyExecutionAttributes),
        testCase(
            ClientObjects.WORKER_VERSION_INFO,
            ProtoObjects.WORKER_VERSION_INFO,
            TypeMapper::workerVersionInfo),
        testCase(
            ClientObjects.START_TIME_FILTER,
            ProtoObjects.START_TIME_FILTER,
            TypeMapper::startTimeFilter),
        testCase(
            ClientObjects.WORKFLOW_EXECUTION_FILTER,
            ProtoObjects.WORKFLOW_EXECUTION_FILTER,
            TypeMapper::workflowExecutionFilter),
        testCase(
            ClientObjects.WORKFLOW_TYPE_FILTER,
            ProtoObjects.WORKFLOW_TYPE_FILTER,
            TypeMapper::workflowTypeFilter),
        testCase(
            WorkflowExecutionCloseStatus.COMPLETED,
            ProtoObjects.STATUS_FILTER,
            TypeMapper::statusFilter),
        testCase(
            ImmutableMap.of("key", ClientObjects.utf8("data")),
            ImmutableMap.of("key", ProtoObjects.payload("data")),
            TypeMapper::payloadByteBufferMap),
        testCase(
            ImmutableMap.of("key", ClientObjects.BAD_BINARY_INFO),
            ImmutableMap.of("key", ProtoObjects.BAD_BINARY_INFO),
            TypeMapper::badBinaryInfoMap),
        testCase(
            ImmutableList.of(ClientObjects.CLUSTER_REPLICATION_CONFIGURATION),
            ImmutableList.of(ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION),
            TypeMapper::clusterReplicationConfigurationArray),
        testCase(
            ImmutableMap.of("key", ClientObjects.WORKFLOW_QUERY_RESULT),
            ImmutableMap.of("key", ProtoObjects.WORKFLOW_QUERY_RESULT),
            TypeMapper::workflowQueryResultMap));
  }

  private static <T, P> Object[] testCase(T from, P to, Function<T, P> via) {
    return new Object[] {from.getClass().getSimpleName(), from, to, via};
  }
}
