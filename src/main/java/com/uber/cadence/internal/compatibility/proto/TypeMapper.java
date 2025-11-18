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
import java.nio.ByteBuffer;
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
    BadBinaryInfo.Builder builder =
        BadBinaryInfo.newBuilder().setCreatedTime(unixNanoToTime(t.getCreatedTimeNano()));
    if (t.getReason() != null) {
      builder.setReason(t.getReason());
    }
    if (t.getOperator() != null) {
      builder.setOperator(t.getOperator());
    }
    return builder.build();
  }

  static Payload payload(byte[] data) {
    if (data == null) {
      return Payload.newBuilder().build();
    }
    return Payload.newBuilder().setData(arrayToByteString(data)).build();
  }

  static Failure failure(String reason, byte[] details) {
    Failure.Builder builder = Failure.newBuilder();
    if (reason != null) {
      builder.setReason(reason);
    }
    if (details != null) {
      builder.setDetails(arrayToByteString(details));
    }
    return builder.build();
  }

  static WorkflowExecution workflowExecution(com.uber.cadence.WorkflowExecution t) {
    if (t == null) {
      return WorkflowExecution.newBuilder().build();
    }
    WorkflowExecution.Builder builder = WorkflowExecution.newBuilder();
    if (t.getWorkflowId() != null) {
      builder.setWorkflowId(t.getWorkflowId());
    }
    if (t.getRunId() != null) {
      builder.setRunId(t.getRunId());
    }
    return builder.build();
  }

  static WorkflowExecution workflowRunPair(String workflowId, String runId) {
    WorkflowExecution.Builder builder = WorkflowExecution.newBuilder();
    if (!Strings.isNullOrEmpty(workflowId)) {
      builder.setWorkflowId(workflowId);
    }
    if (!Strings.isNullOrEmpty(runId)) {
      builder.setRunId(runId);
    }
    return builder.build();
  }

  static ActivityType activityType(com.uber.cadence.ActivityType t) {
    if (t == null) {
      return ActivityType.newBuilder().build();
    }
    ActivityType.Builder builder = ActivityType.newBuilder();
    if (t.getName() != null) {
      builder.setName(t.getName());
    }
    return builder.build();
  }

  static WorkflowType workflowType(com.uber.cadence.WorkflowType t) {
    if (t == null) {
      return WorkflowType.newBuilder().build();
    }
    WorkflowType.Builder builder = WorkflowType.newBuilder();
    if (t.getName() != null) {
      builder.setName(t.getName());
    }
    return builder.build();
  }

  static TaskList taskList(com.uber.cadence.TaskList t) {
    if (t == null) {
      return TaskList.newBuilder().build();
    }
    TaskList.Builder builder = TaskList.newBuilder();
    if (t.getName() != null) {
      builder.setName(t.getName());
    }
    if (t.getKind() != null) {
      builder.setKind(taskListKind(t.getKind()));
    }
    return builder.build();
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
      return SearchAttributes.newBuilder().putAllIndexedFields(Collections.emptyMap()).build();
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
    ClusterReplicationConfiguration.Builder builder = ClusterReplicationConfiguration.newBuilder();
    if (t.getClusterName() != null) {
      builder.setClusterName(t.getClusterName());
    }
    return builder.build();
  }

  static WorkflowQuery workflowQuery(com.uber.cadence.WorkflowQuery t) {
    if (t == null) {
      return null;
    }
    WorkflowQuery.Builder builder = WorkflowQuery.newBuilder();
    if (t.getQueryType() != null) {
      builder.setQueryType(t.getQueryType());
    }
    if (t.getQueryArgs() != null) {
      builder.setQueryArgs(payload(t.getQueryArgs()));
    }
    return builder.build();
  }

  static WorkflowQueryResult workflowQueryResult(com.uber.cadence.WorkflowQueryResult t) {
    if (t == null) {
      return WorkflowQueryResult.newBuilder().build();
    }
    WorkflowQueryResult.Builder builder = WorkflowQueryResult.newBuilder();
    if (t.getResultType() != null) {
      builder.setResultType(queryResultType(t.getResultType()));
    }
    if (t.getAnswer() != null) {
      builder.setAnswer(payload(t.getAnswer()));
    }
    if (t.getErrorMessage() != null) {
      builder.setErrorMessage(t.getErrorMessage());
    }
    return builder.build();
  }

  static StickyExecutionAttributes stickyExecutionAttributes(
      com.uber.cadence.StickyExecutionAttributes t) {
    if (t == null) {
      return StickyExecutionAttributes.newBuilder().build();
    }
    StickyExecutionAttributes.Builder builder =
        StickyExecutionAttributes.newBuilder()
            .setScheduleToStartTimeout(secondsToDuration(t.getScheduleToStartTimeoutSeconds()));
    if (t.getWorkerTaskList() != null) {
      builder.setWorkerTaskList(taskList(t.getWorkerTaskList()));
    }
    return builder.build();
  }

  static WorkerVersionInfo workerVersionInfo(com.uber.cadence.WorkerVersionInfo t) {
    if (t == null) {
      return WorkerVersionInfo.newBuilder().build();
    }
    WorkerVersionInfo.Builder builder = WorkerVersionInfo.newBuilder();
    if (t.getImpl() != null) {
      builder.setImpl(t.getImpl());
    }
    if (t.getFeatureVersion() != null) {
      builder.setFeatureVersion(t.getFeatureVersion());
    }
    return builder.build();
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
    WorkflowExecutionFilter.Builder builder = WorkflowExecutionFilter.newBuilder();
    if (t.getWorkflowId() != null) {
      builder.setWorkflowId(t.getWorkflowId());
    }
    if (t.getRunId() != null) {
      builder.setRunId(t.getRunId());
    }
    return builder.build();
  }

  static WorkflowTypeFilter workflowTypeFilter(com.uber.cadence.WorkflowTypeFilter t) {
    if (t == null) {
      return WorkflowTypeFilter.newBuilder().build();
    }
    WorkflowTypeFilter.Builder builder = WorkflowTypeFilter.newBuilder();
    if (t.getName() != null) {
      builder.setName(t.getName());
    }
    return builder.build();
  }

  static StatusFilter statusFilter(com.uber.cadence.WorkflowExecutionCloseStatus t) {
    if (t == null) {
      return null;
    }
    return StatusFilter.newBuilder().setStatus(workflowExecutionCloseStatus(t)).build();
  }

  static Map<String, Payload> payloadByteBufferMap(Map<String, ByteBuffer> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, Payload> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, payload(t.get(key).array()));
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
