/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.uber.cadence.internal.compatibility.proto.EnumMapper.queryResultType;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.taskListKind;
import static com.uber.cadence.internal.compatibility.proto.EnumMapper.workflowExecutionCloseStatus;
import static com.uber.cadence.internal.compatibility.proto.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.Helpers.fromDoubleValue;
import static com.uber.cadence.internal.compatibility.proto.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.Helpers.unixNanoToTime;

import com.google.common.base.Strings;
import com.uber.cadence.api.v1.ActivityType;
import com.uber.cadence.api.v1.BadBinaries;
import com.uber.cadence.api.v1.BadBinaryInfo;
import com.uber.cadence.api.v1.ClusterReplicationConfiguration;
import com.uber.cadence.api.v1.Failure;
import com.uber.cadence.api.v1.Header;
import com.uber.cadence.api.v1.Memo;
import com.uber.cadence.api.v1.Payload;
import com.uber.cadence.api.v1.RetryPolicy;
import com.uber.cadence.api.v1.SearchAttributes;
import com.uber.cadence.api.v1.StartTimeFilter;
import com.uber.cadence.api.v1.StatusFilter;
import com.uber.cadence.api.v1.StickyExecutionAttributes;
import com.uber.cadence.api.v1.TaskList;
import com.uber.cadence.api.v1.TaskListMetadata;
import com.uber.cadence.api.v1.WorkerVersionInfo;
import com.uber.cadence.api.v1.WorkflowExecution;
import com.uber.cadence.api.v1.WorkflowExecutionFilter;
import com.uber.cadence.api.v1.WorkflowQuery;
import com.uber.cadence.api.v1.WorkflowQueryResult;
import com.uber.cadence.api.v1.WorkflowType;
import com.uber.cadence.api.v1.WorkflowTypeFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TypeMapper {

  static BadBinaryInfo badBinaryInfo(com.uber.cadence.BadBinaryInfo t) {
    if (t == null) {
      return null;
    }
    return BadBinaryInfo.newBuilder()
        .setReason(t.getReason())
        .setOperator(t.getOperator())
        .setCreatedTime(unixNanoToTime(t.getCreatedTimeNano()))
        .build();
  }

  static Payload payload(byte[] data) {
    if (data == null) {
      return Payload.newBuilder().build();
    }
    return Payload.newBuilder().setData(arrayToByteString(data)).build();
  }

  static Failure failure(String reason, byte[] details) {
    if (reason == null) {
      return Failure.newBuilder().build();
    }
    return Failure.newBuilder().setReason(reason).setDetails(arrayToByteString(details)).build();
  }

  static WorkflowExecution workflowExecution(com.uber.cadence.WorkflowExecution t) {
    if (t == null) {
      return WorkflowExecution.newBuilder().build();
    }
    if (t.getWorkflowId() == null && t.getRunId() == null) {
      return WorkflowExecution.newBuilder().build();
    }
    WorkflowExecution.Builder builder =
        WorkflowExecution.newBuilder().setWorkflowId(t.getWorkflowId());
    if (t.getRunId() != null) {
      builder.setRunId(t.getRunId());
    }
    return builder.build();
  }

  static WorkflowExecution workflowRunPair(String workflowId, String runId) {
    if (Strings.isNullOrEmpty(workflowId) && Strings.isNullOrEmpty(runId)) {
      return WorkflowExecution.newBuilder().build();
    }
    return WorkflowExecution.newBuilder().setWorkflowId(workflowId).setRunId(runId).build();
  }

  static ActivityType activityType(com.uber.cadence.ActivityType t) {
    if (t == null) {
      return ActivityType.newBuilder().build();
    }
    return ActivityType.newBuilder().setName(t.getName()).build();
  }

  static WorkflowType workflowType(com.uber.cadence.WorkflowType t) {
    if (t == null) {
      return WorkflowType.newBuilder().build();
    }
    return WorkflowType.newBuilder().setName(t.getName()).build();
  }

  static TaskList taskList(com.uber.cadence.TaskList t) {
    if (t == null) {
      return TaskList.newBuilder().build();
    }
    return TaskList.newBuilder().setName(t.getName()).setKind(taskListKind(t.getKind())).build();
  }

  static TaskListMetadata taskListMetadata(com.uber.cadence.TaskListMetadata t) {
    if (t == null) {
      return TaskListMetadata.newBuilder().build();
    }
    return TaskListMetadata.newBuilder()
        .setMaxTasksPerSecond(fromDoubleValue(t.getMaxTasksPerSecond()))
        .build();
  }

  static RetryPolicy retryPolicy(com.uber.cadence.RetryPolicy t) {
    if (t == null) {
      return null;
    }
    RetryPolicy.Builder builder =
        RetryPolicy.newBuilder()
            .setInitialInterval(secondsToDuration(t.getInitialIntervalInSeconds()))
            .setBackoffCoefficient(t.getBackoffCoefficient())
            .setMaximumInterval(secondsToDuration(t.getMaximumIntervalInSeconds()))
            .setMaximumAttempts(t.getMaximumAttempts())
            .setExpirationInterval(secondsToDuration(t.getExpirationIntervalInSeconds()));
    if (t.getNonRetriableErrorReasons() != null) {
      builder.addAllNonRetryableErrorReasons(t.getNonRetriableErrorReasons());
    }
    return builder.build();
  }

  static Header header(com.uber.cadence.Header t) {
    if (t == null) {
      return Header.newBuilder().build();
    }
    return Header.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static Memo memo(com.uber.cadence.Memo t) {
    if (t == null) {
      return Memo.newBuilder().build();
    }
    return Memo.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static SearchAttributes searchAttributes(com.uber.cadence.SearchAttributes t) {
    if (t == null) {
      return SearchAttributes.newBuilder().build();
    }
    return SearchAttributes.newBuilder()
        .putAllIndexedFields(payloadByteBufferMap(t.getIndexedFields()))
        .build();
  }

  static BadBinaries badBinaries(com.uber.cadence.BadBinaries t) {
    if (t == null) {
      return BadBinaries.newBuilder().build();
    }
    return BadBinaries.newBuilder().putAllBinaries(badBinaryInfoMap(t.getBinaries())).build();
  }

  static ClusterReplicationConfiguration clusterReplicationConfiguration(
      com.uber.cadence.ClusterReplicationConfiguration t) {
    if (t == null) {
      return ClusterReplicationConfiguration.newBuilder().build();
    }
    return ClusterReplicationConfiguration.newBuilder().setClusterName(t.getClusterName()).build();
  }

  static WorkflowQuery workflowQuery(com.uber.cadence.WorkflowQuery t) {
    if (t == null) {
      return null;
    }
    return WorkflowQuery.newBuilder()
        .setQueryType(t.getQueryType())
        .setQueryArgs(payload(t.getQueryArgs()))
        .build();
  }

  static WorkflowQueryResult workflowQueryResult(com.uber.cadence.WorkflowQueryResult t) {
    if (t == null) {
      return WorkflowQueryResult.newBuilder().build();
    }
    return WorkflowQueryResult.newBuilder()
        .setResultType(queryResultType(t.getResultType()))
        .setAnswer(payload(t.getAnswer()))
        .setErrorMessage(t.getErrorMessage())
        .build();
  }

  static StickyExecutionAttributes stickyExecutionAttributes(
      com.uber.cadence.StickyExecutionAttributes t) {
    if (t == null) {
      return StickyExecutionAttributes.newBuilder().build();
    }
    return StickyExecutionAttributes.newBuilder()
        .setWorkerTaskList(taskList(t.getWorkerTaskList()))
        .setScheduleToStartTimeout(secondsToDuration(t.getScheduleToStartTimeoutSeconds()))
        .build();
  }

  static WorkerVersionInfo workerVersionInfo(com.uber.cadence.WorkerVersionInfo t) {
    if (t == null) {
      return WorkerVersionInfo.newBuilder().build();
    }
    return WorkerVersionInfo.newBuilder()
        .setImpl(t.getImpl())
        .setFeatureVersion(t.getFeatureVersion())
        .build();
  }

  static StartTimeFilter startTimeFilter(com.uber.cadence.StartTimeFilter t) {
    if (t == null) {
      return null;
    }
    return StartTimeFilter.newBuilder()
        .setEarliestTime(unixNanoToTime(t.getEarliestTime()))
        .setLatestTime(unixNanoToTime(t.getLatestTime()))
        .build();
  }

  static WorkflowExecutionFilter workflowExecutionFilter(
      com.uber.cadence.WorkflowExecutionFilter t) {
    if (t == null) {
      return WorkflowExecutionFilter.newBuilder().build();
    }
    return WorkflowExecutionFilter.newBuilder()
        .setWorkflowId(t.getWorkflowId())
        .setRunId(t.getRunId())
        .build();
  }

  static WorkflowTypeFilter workflowTypeFilter(com.uber.cadence.WorkflowTypeFilter t) {
    if (t == null) {
      return WorkflowTypeFilter.newBuilder().build();
    }
    return WorkflowTypeFilter.newBuilder().setName(t.getName()).build();
  }

  static StatusFilter statusFilter(com.uber.cadence.WorkflowExecutionCloseStatus t) {
    if (t == null) {
      return null;
    }
    return StatusFilter.newBuilder().setStatus(workflowExecutionCloseStatus(t)).build();
  }

  static Map<String, Payload> payloadByteBufferMap(Map<String, byte[]> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, Payload> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, payload(t.get(key)));
    }
    return v;
  }

  static Map<String, BadBinaryInfo> badBinaryInfoMap(
      Map<String, com.uber.cadence.BadBinaryInfo> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, BadBinaryInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, badBinaryInfo(t.get(key)));
    }
    return v;
  }

  static List<ClusterReplicationConfiguration> clusterReplicationConfigurationArray(
      List<com.uber.cadence.ClusterReplicationConfiguration> t) {
    if (t == null) {
      return Collections.emptyList();
    }
    List<ClusterReplicationConfiguration> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(clusterReplicationConfiguration(t.get(i)));
    }
    return v;
  }

  static Map<String, WorkflowQueryResult> workflowQueryResultMap(
      Map<String, com.uber.cadence.WorkflowQueryResult> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, WorkflowQueryResult> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, workflowQueryResult(t.get(key)));
    }
    return v;
  }
}
