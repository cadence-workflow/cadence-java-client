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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.uber.cadence.WorkflowExecutionCloseStatus;
import com.uber.cadence.internal.compatibility.ProtoObjects;
import com.uber.cadence.internal.compatibility.ThriftObjects;
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
            ThriftObjects.BAD_BINARY_INFO, ProtoObjects.BAD_BINARY_INFO, TypeMapper::badBinaryInfo),
        testCase(
            ThriftObjects.utf8Bytes("data"), ProtoObjects.payload("data"), TypeMapper::payload),
        testCase(ThriftObjects.ACTIVITY_TYPE, ProtoObjects.ACTIVITY_TYPE, TypeMapper::activityType),
        testCase(ThriftObjects.WORKFLOW_TYPE, ProtoObjects.WORKFLOW_TYPE, TypeMapper::workflowType),
        testCase(ThriftObjects.TASK_LIST, ProtoObjects.TASK_LIST, TypeMapper::taskList),
        testCase(
            ThriftObjects.TASK_LIST_METADATA,
            ProtoObjects.TASK_LIST_METADATA,
            TypeMapper::taskListMetadata),
        testCase(ThriftObjects.RETRY_POLICY, ProtoObjects.RETRY_POLICY, TypeMapper::retryPolicy),
        testCase(ThriftObjects.HEADER, ProtoObjects.HEADER, TypeMapper::header),
        testCase(ThriftObjects.MEMO, ProtoObjects.MEMO, TypeMapper::memo),
        testCase(
            ThriftObjects.SEARCH_ATTRIBUTES,
            ProtoObjects.SEARCH_ATTRIBUTES,
            TypeMapper::searchAttributes),
        testCase(ThriftObjects.BAD_BINARIES, ProtoObjects.BAD_BINARIES, TypeMapper::badBinaries),
        testCase(
            ThriftObjects.CLUSTER_REPLICATION_CONFIGURATION,
            ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION,
            TypeMapper::clusterReplicationConfiguration),
        testCase(
            ThriftObjects.WORKFLOW_QUERY, ProtoObjects.WORKFLOW_QUERY, TypeMapper::workflowQuery),
        testCase(
            ThriftObjects.WORKFLOW_QUERY_RESULT,
            ProtoObjects.WORKFLOW_QUERY_RESULT,
            TypeMapper::workflowQueryResult),
        testCase(
            ThriftObjects.STICKY_EXECUTION_ATTRIBUTES,
            ProtoObjects.STICKY_EXECUTION_ATTRIBUTES,
            TypeMapper::stickyExecutionAttributes),
        testCase(
            ThriftObjects.WORKER_VERSION_INFO,
            ProtoObjects.WORKER_VERSION_INFO,
            TypeMapper::workerVersionInfo),
        testCase(
            ThriftObjects.START_TIME_FILTER,
            ProtoObjects.START_TIME_FILTER,
            TypeMapper::startTimeFilter),
        testCase(
            ThriftObjects.WORKFLOW_EXECUTION_FILTER,
            ProtoObjects.WORKFLOW_EXECUTION_FILTER,
            TypeMapper::workflowExecutionFilter),
        testCase(
            ThriftObjects.WORKFLOW_TYPE_FILTER,
            ProtoObjects.WORKFLOW_TYPE_FILTER,
            TypeMapper::workflowTypeFilter),
        testCase(
            WorkflowExecutionCloseStatus.COMPLETED,
            ProtoObjects.STATUS_FILTER,
            TypeMapper::statusFilter),
        testCase(
            ImmutableMap.of("key", ThriftObjects.utf8("data")),
            ImmutableMap.of("key", ProtoObjects.payload("data")),
            TypeMapper::payloadByteBufferMap),
        testCase(
            ImmutableMap.of("key", ThriftObjects.BAD_BINARY_INFO),
            ImmutableMap.of("key", ProtoObjects.BAD_BINARY_INFO),
            TypeMapper::badBinaryInfoMap),
        testCase(
            ImmutableList.of(ThriftObjects.CLUSTER_REPLICATION_CONFIGURATION),
            ImmutableList.of(ProtoObjects.CLUSTER_REPLICATION_CONFIGURATION),
            TypeMapper::clusterReplicationConfigurationArray),
        testCase(
            ImmutableMap.of("key", ThriftObjects.WORKFLOW_QUERY_RESULT),
            ImmutableMap.of("key", ProtoObjects.WORKFLOW_QUERY_RESULT),
            TypeMapper::workflowQueryResultMap));
  }

  private static <T, P> Object[] testCase(T from, P to, Function<T, P> via) {
    return new Object[] {from.getClass().getSimpleName(), from, to, via};
  }
}
