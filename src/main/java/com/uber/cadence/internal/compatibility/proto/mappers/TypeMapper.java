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
package com.uber.cadence.internal.compatibility.proto.mappers;

import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.domainStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.encodingType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.indexedValueType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.pendingActivityState;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.pendingDecisionState;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.queryResultType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.taskListKind;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.workflowExecutionCloseStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.durationToDays;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.fromDoubleValue;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.timeToUnixNano;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.unixNanoToTime;

import com.google.common.base.Strings;
import com.uber.cadence.api.v1.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TypeMapper {

  static BadBinaryInfo badBinaryInfo(com.uber.cadence.entities.BadBinaryInfo t) {
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

  static WorkflowExecution workflowExecution(com.uber.cadence.entities.WorkflowExecution t) {
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

  static ActivityType activityType(com.uber.cadence.entities.ActivityType t) {
    if (t == null) {
      return ActivityType.newBuilder().build();
    }
    return ActivityType.newBuilder().setName(t.getName()).build();
  }

  static WorkflowType workflowType(com.uber.cadence.entities.WorkflowType t) {
    if (t == null) {
      return WorkflowType.newBuilder().build();
    }
    return WorkflowType.newBuilder().setName(t.getName()).build();
  }

  static TaskList taskList(com.uber.cadence.entities.TaskList t) {
    if (t == null) {
      return TaskList.newBuilder().build();
    }
    return TaskList.newBuilder().setName(t.getName()).setKind(taskListKind(t.getKind())).build();
  }

  static TaskListMetadata taskListMetadata(com.uber.cadence.entities.TaskListMetadata t) {
    if (t == null) {
      return TaskListMetadata.newBuilder().build();
    }
    return TaskListMetadata.newBuilder()
        .setMaxTasksPerSecond(fromDoubleValue(t.getMaxTasksPerSecond()))
        .build();
  }

  static RetryPolicy retryPolicy(com.uber.cadence.entities.RetryPolicy t) {
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

  static Header header(com.uber.cadence.entities.Header t) {
    if (t == null) {
      return Header.newBuilder().build();
    }
    return Header.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static Memo memo(com.uber.cadence.entities.Memo t) {
    if (t == null) {
      return Memo.newBuilder().build();
    }
    return Memo.newBuilder().putAllFields(payloadByteBufferMap(t.getFields())).build();
  }

  static SearchAttributes searchAttributes(com.uber.cadence.entities.SearchAttributes t) {
    if (t == null) {
      return SearchAttributes.newBuilder().build();
    }
    return SearchAttributes.newBuilder()
        .putAllIndexedFields(payloadByteBufferMap(t.getIndexedFields()))
        .build();
  }

  static BadBinaries badBinaries(com.uber.cadence.entities.BadBinaries t) {
    if (t == null) {
      return BadBinaries.newBuilder().build();
    }
    return BadBinaries.newBuilder().putAllBinaries(badBinaryInfoMap(t.getBinaries())).build();
  }

  static ClusterReplicationConfiguration clusterReplicationConfiguration(
      com.uber.cadence.entities.ClusterReplicationConfiguration t) {
    if (t == null) {
      return ClusterReplicationConfiguration.newBuilder().build();
    }
    return ClusterReplicationConfiguration.newBuilder().setClusterName(t.getClusterName()).build();
  }

  static WorkflowQuery workflowQuery(com.uber.cadence.entities.WorkflowQuery t) {
    if (t == null) {
      return null;
    }
    return WorkflowQuery.newBuilder()
        .setQueryType(t.getQueryType())
        .setQueryArgs(payload(t.getQueryArgs()))
        .build();
  }

  static WorkflowQueryResult workflowQueryResult(com.uber.cadence.entities.WorkflowQueryResult t) {
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
      com.uber.cadence.entities.StickyExecutionAttributes t) {
    if (t == null) {
      return StickyExecutionAttributes.newBuilder().build();
    }
    return StickyExecutionAttributes.newBuilder()
        .setWorkerTaskList(taskList(t.getWorkerTaskList()))
        .setScheduleToStartTimeout(secondsToDuration(t.getScheduleToStartTimeoutSeconds()))
        .build();
  }

  static WorkerVersionInfo workerVersionInfo(com.uber.cadence.entities.WorkerVersionInfo t) {
    if (t == null) {
      return WorkerVersionInfo.newBuilder().build();
    }
    return WorkerVersionInfo.newBuilder()
        .setImpl(t.getImpl())
        .setFeatureVersion(t.getFeatureVersion())
        .build();
  }

  static StartTimeFilter startTimeFilter(com.uber.cadence.entities.StartTimeFilter t) {
    if (t == null) {
      return null;
    }
    return StartTimeFilter.newBuilder()
        .setEarliestTime(unixNanoToTime(t.getEarliestTime()))
        .setLatestTime(unixNanoToTime(t.getLatestTime()))
        .build();
  }

  static WorkflowExecutionFilter workflowExecutionFilter(
      com.uber.cadence.entities.WorkflowExecutionFilter t) {
    if (t == null) {
      return WorkflowExecutionFilter.newBuilder().build();
    }
    return WorkflowExecutionFilter.newBuilder()
        .setWorkflowId(t.getWorkflowId())
        .setRunId(t.getRunId())
        .build();
  }

  static WorkflowTypeFilter workflowTypeFilter(com.uber.cadence.entities.WorkflowTypeFilter t) {
    if (t == null) {
      return WorkflowTypeFilter.newBuilder().build();
    }
    return WorkflowTypeFilter.newBuilder().setName(t.getName()).build();
  }

  static StatusFilter statusFilter(com.uber.cadence.entities.WorkflowExecutionCloseStatus t) {
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
      Map<String, com.uber.cadence.entities.BadBinaryInfo> t) {
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
      List<com.uber.cadence.entities.ClusterReplicationConfiguration> t) {
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
      Map<String, com.uber.cadence.entities.WorkflowQueryResult> t) {
    if (t == null) {
      return Collections.emptyMap();
    }
    Map<String, WorkflowQueryResult> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, workflowQueryResult(t.get(key)));
    }
    return v;
  }

  static byte[] payload(Payload t) {
    if (t == null || t == Payload.getDefaultInstance()) {
      return null;
    }
    if (t.getData() == null || t.getData().size() == 0) {
      // protoPayload will not generate this case
      // however, Data field will be dropped by the encoding if it's empty
      // and receiver side will see null for the Data field
      // since we already know p is not null, Data field must be an empty byte array
      return new byte[0];
    }
    return byteStringToArray(t.getData());
  }

  static String failureReason(Failure t) {
    if (t == null || t == Failure.getDefaultInstance()) {
      return null;
    }
    return t.getReason();
  }

  static byte[] failureDetails(Failure t) {
    if (t == null || t == Failure.getDefaultInstance()) {
      return null;
    }
    return byteStringToArray(t.getDetails());
  }

  static com.uber.cadence.entities.WorkflowExecution workflowExecution(WorkflowExecution t) {
    if (t == null || t == WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.WorkflowExecution we = new com.uber.cadence.entities.WorkflowExecution();
    we.setWorkflowId(t.getWorkflowId());
    we.setRunId(t.getRunId());
    return we;
  }

  static String workflowId(WorkflowExecution t) {
    if (t == null || t == WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    return t.getWorkflowId();
  }

  static String runId(WorkflowExecution t) {
    if (t == null || t == WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    return t.getRunId();
  }

  static com.uber.cadence.entities.ActivityType activityType(ActivityType t) {
    if (t == null || t == ActivityType.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.ActivityType activityType = new com.uber.cadence.entities.ActivityType();
    activityType.setName(t.getName());
    return activityType;
  }

  static com.uber.cadence.entities.WorkflowType workflowType(WorkflowType t) {
    if (t == null || t == WorkflowType.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.WorkflowType wt = new com.uber.cadence.entities.WorkflowType();
    wt.setName(t.getName());
    return wt;
  }

  static com.uber.cadence.entities.TaskList taskList(TaskList t) {
    if (t == null || t == TaskList.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.TaskList taskList = new com.uber.cadence.entities.TaskList();
    taskList.setName(t.getName());
    taskList.setKind(taskListKind(t.getKind()));
    return taskList;
  }

  static com.uber.cadence.entities.RetryPolicy retryPolicy(RetryPolicy t) {
    if (t == null || t == RetryPolicy.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.RetryPolicy res = new com.uber.cadence.entities.RetryPolicy();
    res.setInitialIntervalInSeconds(durationToSeconds(t.getInitialInterval()));
    res.setBackoffCoefficient(t.getBackoffCoefficient());
    res.setMaximumIntervalInSeconds(durationToSeconds(t.getMaximumInterval()));
    res.setMaximumAttempts(t.getMaximumAttempts());
    res.setNonRetriableErrorReasons(t.getNonRetryableErrorReasonsList());
    res.setExpirationIntervalInSeconds(durationToSeconds(t.getExpirationInterval()));
    return res;
  }

  static com.uber.cadence.entities.Header header(Header t) {
    if (t == null || t == Header.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.Header res = new com.uber.cadence.entities.Header();
    res.setFields(payloadMap(t.getFieldsMap()));
    return res;
  }

  static com.uber.cadence.entities.Memo memo(Memo t) {
    if (t == null || t == Memo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.Memo res = new com.uber.cadence.entities.Memo();
    res.setFields(payloadMap(t.getFieldsMap()));
    return res;
  }

  static com.uber.cadence.entities.SearchAttributes searchAttributes(SearchAttributes t) {
    if (t == null
        || t.getAllFields().size() == 0
        || t == SearchAttributes.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.SearchAttributes res = new com.uber.cadence.entities.SearchAttributes();
    res.setIndexedFields(payloadMap(t.getIndexedFieldsMap()));
    return res;
  }

  static com.uber.cadence.entities.BadBinaries badBinaries(BadBinaries t) {
    if (t == null || t == BadBinaries.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.BadBinaries badBinaries = new com.uber.cadence.entities.BadBinaries();
    badBinaries.setBinaries(badBinaryInfoMapFromProto(t.getBinariesMap()));
    return badBinaries;
  }

  static com.uber.cadence.entities.BadBinaryInfo badBinaryInfo(BadBinaryInfo t) {
    if (t == null || t == BadBinaryInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.BadBinaryInfo res = new com.uber.cadence.entities.BadBinaryInfo();
    res.setReason(t.getReason());
    res.setOperator(t.getOperator());
    res.setCreatedTimeNano(timeToUnixNano(t.getCreatedTime()));
    return res;
  }

  static Map<String, com.uber.cadence.entities.BadBinaryInfo> badBinaryInfoMapFromProto(
      Map<String, BadBinaryInfo> t) {
    if (t == null) {
      return null;
    }
    Map<String, com.uber.cadence.entities.BadBinaryInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, badBinaryInfo(t.get(key)));
    }
    return v;
  }

  static com.uber.cadence.entities.WorkflowQuery workflowQuery(WorkflowQuery t) {
    if (t == null || t == WorkflowQuery.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.WorkflowQuery res = new com.uber.cadence.entities.WorkflowQuery();
    res.setQueryType(t.getQueryType());
    res.setQueryArgs(payload(t.getQueryArgs()));
    return res;
  }

  static Map<String, ByteBuffer> payloadMap(Map<String, Payload> t) {
    if (t == null) {
      return null;
    }
    Map<String, ByteBuffer> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, ByteBuffer.wrap(payload(t.get(key))));
    }
    return v;
  }

  static List<com.uber.cadence.entities.ClusterReplicationConfiguration> clusterReplicationConfigurationArrayFromProto(
      List<ClusterReplicationConfiguration> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.ClusterReplicationConfiguration> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(clusterReplicationConfiguration(t.get(i)));
    }
    return v;
  }

  static com.uber.cadence.entities.ClusterReplicationConfiguration clusterReplicationConfiguration(
      ClusterReplicationConfiguration t) {
    if (t == null
        || t == ClusterReplicationConfiguration.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.ClusterReplicationConfiguration res = new com.uber.cadence.entities.ClusterReplicationConfiguration();
    res.setClusterName(t.getClusterName());
    return res;
  }

  static com.uber.cadence.entities.DataBlob dataBlob(DataBlob t) {
    if (t == null || t == DataBlob.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.DataBlob dataBlob = new com.uber.cadence.entities.DataBlob();
    dataBlob.setEncodingType(encodingType(t.getEncodingType()));
    dataBlob.setData(byteStringToArray(t.getData()));
    return dataBlob;
  }

  static long externalInitiatedId(ExternalExecutionInfo t) {
    return t.getInitiatedId();
  }

  static com.uber.cadence.entities.WorkflowExecution externalWorkflowExecution(
      ExternalExecutionInfo t) {
    if (t == null || t == ExternalExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return workflowExecution(t.getWorkflowExecution());
  }

  static com.uber.cadence.entities.ResetPoints resetPoints(ResetPoints t) {
    if (t == null || t == ResetPoints.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.ResetPoints res = new com.uber.cadence.entities.ResetPoints();
    res.setPoints(resetPointInfoArray(t.getPointsList()));
    return res;
  }

  static com.uber.cadence.entities.ResetPointInfo resetPointInfo(ResetPointInfo t) {
    if (t == null || t == ResetPointInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.ResetPointInfo res = new com.uber.cadence.entities.ResetPointInfo();
    res.setBinaryChecksum(t.getBinaryChecksum());
    res.setRunId(t.getRunId());
    res.setFirstDecisionCompletedId(t.getFirstDecisionCompletedId());
    res.setCreatedTimeNano(timeToUnixNano(t.getCreatedTime()));
    res.setExpiringTimeNano(timeToUnixNano(t.getExpiringTime()));
    res.setResettable(t.getResettable());
    return res;
  }

  static com.uber.cadence.entities.PollerInfo pollerInfo(PollerInfo t) {
    if (t == null || t == PollerInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.PollerInfo res = new com.uber.cadence.entities.PollerInfo();
    res.setLastAccessTime(timeToUnixNano(t.getLastAccessTime()));
    res.setIdentity(t.getIdentity());
    res.setRatePerSecond(t.getRatePerSecond());
    return res;
  }

  static com.uber.cadence.entities.TaskListStatus taskListStatus(TaskListStatus t) {
    if (t == null || t == TaskListStatus.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.TaskListStatus res = new com.uber.cadence.entities.TaskListStatus();
    res.setBacklogCountHint(t.getBacklogCountHint());
    res.setReadLevel(t.getReadLevel());
    res.setAckLevel(t.getAckLevel());
    res.setRatePerSecond(t.getRatePerSecond());
    res.setTaskIDBlock(taskIdBlock(t.getTaskIdBlock()));
    return res;
  }

  static com.uber.cadence.entities.TaskIDBlock taskIdBlock(TaskIDBlock t) {
    if (t == null || t == TaskIDBlock.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.TaskIDBlock res = new com.uber.cadence.entities.TaskIDBlock();
    res.setStartID(t.getStartId());
    res.setEndID(t.getEndId());
    return res;
  }

  static com.uber.cadence.entities.WorkflowExecutionConfiguration workflowExecutionConfiguration(
      WorkflowExecutionConfiguration t) {
    if (t == null
        || t == WorkflowExecutionConfiguration.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.WorkflowExecutionConfiguration res = new com.uber.cadence.entities.WorkflowExecutionConfiguration();
    res.setTaskList(taskList(t.getTaskList()));
    res.setExecutionStartToCloseTimeoutSeconds(
        durationToSeconds(t.getExecutionStartToCloseTimeout()));
    res.setTaskStartToCloseTimeoutSeconds(durationToSeconds(t.getTaskStartToCloseTimeout()));
    return res;
  }

  static com.uber.cadence.entities.WorkflowExecutionInfo workflowExecutionInfo(
      WorkflowExecutionInfo t) {
    if (t == null || t == WorkflowExecutionInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.WorkflowExecutionInfo res = new com.uber.cadence.entities.WorkflowExecutionInfo();
    res.setExecution(workflowExecution(t.getWorkflowExecution()));
    res.setType(workflowType(t.getType()));
    res.setStartTime(timeToUnixNano(t.getStartTime()));
    res.setCloseTime(timeToUnixNano(t.getCloseTime()));
    res.setCloseStatus(workflowExecutionCloseStatus(t.getCloseStatus()));
    res.setHistoryLength(t.getHistoryLength());
    res.setParentDomainName(parentDomainName(t.getParentExecutionInfo()));
    res.setParentDomainId(parentDomainId(t.getParentExecutionInfo()));
    res.setParentExecution(parentWorkflowExecution(t.getParentExecutionInfo()));
    res.setExecutionTime(timeToUnixNano(t.getExecutionTime()));
    res.setMemo(memo(t.getMemo()));
    res.setSearchAttributes(searchAttributes(t.getSearchAttributes()));
    res.setAutoResetPoints(resetPoints(t.getAutoResetPoints()));
    res.setTaskList(t.getTaskList());
    res.setIsCron(t.getIsCron());
    return res;
  }

  static String parentDomainId(ParentExecutionInfo t) {
    if (t == null || t == ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return t.getDomainId();
  }

  static String parentDomainName(ParentExecutionInfo t) {
    if (t == null || t == ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return t.getDomainName();
  }

  static long parentInitiatedId(ParentExecutionInfo t) {
    if (t == null || t == ParentExecutionInfo.getDefaultInstance()) {
      return -1;
    }
    return t.getInitiatedId();
  }

  static com.uber.cadence.entities.WorkflowExecution parentWorkflowExecution(ParentExecutionInfo t) {
    if (t == null || t == ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return workflowExecution(t.getWorkflowExecution());
  }

  static com.uber.cadence.entities.PendingActivityInfo pendingActivityInfo(PendingActivityInfo t) {
    if (t == null || t == PendingActivityInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.PendingActivityInfo res = new com.uber.cadence.entities.PendingActivityInfo();
    res.setActivityID(t.getActivityId());
    res.setActivityType(activityType(t.getActivityType()));
    res.setState(pendingActivityState(t.getState()));
    res.setHeartbeatDetails(payload(t.getHeartbeatDetails()));
    res.setLastHeartbeatTimestamp(timeToUnixNano(t.getLastHeartbeatTime()));
    res.setLastStartedTimestamp(timeToUnixNano(t.getLastStartedTime()));
    res.setAttempt(t.getAttempt());
    res.setMaximumAttempts(t.getMaximumAttempts());
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setExpirationTimestamp(timeToUnixNano(t.getExpirationTime()));
    res.setLastFailureReason(failureReason(t.getLastFailure()));
    res.setLastFailureDetails(failureDetails(t.getLastFailure()));
    res.setLastWorkerIdentity(t.getLastWorkerIdentity());
    return res;
  }

  static com.uber.cadence.entities.PendingChildExecutionInfo pendingChildExecutionInfo(
      PendingChildExecutionInfo t) {
    if (t == null || t == PendingChildExecutionInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.PendingChildExecutionInfo res = new com.uber.cadence.entities.PendingChildExecutionInfo();
    res.setWorkflowID(workflowId(t.getWorkflowExecution()));
    res.setRunID(runId(t.getWorkflowExecution()));
    res.setWorkflowTypName(t.getWorkflowTypeName());
    res.setInitiatedID(t.getInitiatedId());
    res.setParentClosePolicy(parentClosePolicy(t.getParentClosePolicy()));
    return res;
  }

  static com.uber.cadence.entities.PendingDecisionInfo pendingDecisionInfo(PendingDecisionInfo t) {
    if (t == null || t == PendingDecisionInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.PendingDecisionInfo res = new com.uber.cadence.entities.PendingDecisionInfo();
    res.setState(pendingDecisionState(t.getState()));
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setAttempt(t.getAttempt());
    res.setOriginalScheduledTimestamp(timeToUnixNano(t.getOriginalScheduledTime()));
    return res;
  }

  static com.uber.cadence.entities.ActivityLocalDispatchInfo activityLocalDispatchInfo(
      ActivityLocalDispatchInfo t) {
    if (t == null || t == ActivityLocalDispatchInfo.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.ActivityLocalDispatchInfo res = new com.uber.cadence.entities.ActivityLocalDispatchInfo();
    res.setActivityId(t.getActivityId());
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setScheduledTimestampOfThisAttempt(timeToUnixNano(t.getScheduledTimeOfThisAttempt()));
    res.setTaskToken(byteStringToArray(t.getTaskToken()));
    return res;
  }

  static com.uber.cadence.entities.SupportedClientVersions supportedClientVersions(
      SupportedClientVersions t) {
    if (t == null || t == SupportedClientVersions.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.SupportedClientVersions res = new com.uber.cadence.entities.SupportedClientVersions();
    res.setGoSdk(t.getGoSdk());
    res.setJavaSdk(t.getJavaSdk());
    return res;
  }

  static com.uber.cadence.entities.DescribeDomainResponse describeDomainResponseDomain(Domain t) {
    if (t == null || t == Domain.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.DescribeDomainResponse res = new com.uber.cadence.entities.DescribeDomainResponse();
    com.uber.cadence.entities.DomainInfo domainInfo = new com.uber.cadence.entities.DomainInfo();
    res.setDomainInfo(domainInfo);

    domainInfo.setName(t.getName());
    domainInfo.setStatus(domainStatus(t.getStatus()));
    domainInfo.setDescription(t.getDescription());
    domainInfo.setOwnerEmail(t.getOwnerEmail());
    domainInfo.setData(t.getDataMap());
    domainInfo.setUuid(t.getId());

    com.uber.cadence.entities.DomainConfiguration domainConfiguration = new com.uber.cadence.entities.DomainConfiguration();
    res.setConfiguration(domainConfiguration);

    domainConfiguration.setWorkflowExecutionRetentionPeriodInDays(
        durationToDays(t.getWorkflowExecutionRetentionPeriod()));
    domainConfiguration.setEmitMetric(true);
    domainConfiguration.setBadBinaries(badBinaries(t.getBadBinaries()));
    domainConfiguration.setHistoryArchivalStatus(archivalStatus(t.getHistoryArchivalStatus()));
    domainConfiguration.setHistoryArchivalURI(t.getHistoryArchivalUri());
    domainConfiguration.setVisibilityArchivalStatus(
        archivalStatus(t.getVisibilityArchivalStatus()));
    domainConfiguration.setVisibilityArchivalURI(t.getVisibilityArchivalUri());

    com.uber.cadence.entities.DomainReplicationConfiguration domainReplicationConfiguration =
        new com.uber.cadence.entities.DomainReplicationConfiguration();
    res.setReplicationConfiguration(domainReplicationConfiguration);

    domainReplicationConfiguration.setActiveClusterName(t.getActiveClusterName());
    domainReplicationConfiguration.setClusters(
        clusterReplicationConfigurationArrayFromProto(t.getClustersList()));
    res.setFailoverVersion(t.getFailoverVersion());
    res.setIsGlobalDomain(t.getIsGlobalDomain());

    return res;
  }

  static com.uber.cadence.entities.TaskListMetadata taskListMetadata(TaskListMetadata t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.TaskListMetadata res = new com.uber.cadence.entities.TaskListMetadata();
    res.setMaxTasksPerSecond(t.getMaxTasksPerSecond().getValue());
    return res;
  }

  static com.uber.cadence.entities.TaskListPartitionMetadata taskListPartitionMetadata(
      TaskListPartitionMetadata t) {
    if (t == null || t == TaskListPartitionMetadata.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.TaskListPartitionMetadata res = new com.uber.cadence.entities.TaskListPartitionMetadata();
    res.setKey(t.getKey());
    res.setOwnerHostName(t.getOwnerHostName());
    return res;
  }

  static com.uber.cadence.entities.QueryRejected queryRejected(QueryRejected t) {
    if (t == null || t == QueryRejected.getDefaultInstance()) {
      return null;
    }
    com.uber.cadence.entities.QueryRejected res = new com.uber.cadence.entities.QueryRejected();
    res.setCloseStatus(workflowExecutionCloseStatus(t.getCloseStatus()));
    return res;
  }

  static List<com.uber.cadence.entities.PollerInfo> pollerInfoArray(List<PollerInfo> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.PollerInfo> v = new ArrayList<>();
    for (PollerInfo pollerInfo : t) {
      v.add(pollerInfo(pollerInfo));
    }
    return v;
  }

  static List<com.uber.cadence.entities.ResetPointInfo> resetPointInfoArray(List<ResetPointInfo> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.ResetPointInfo> v = new ArrayList<>();
    for (ResetPointInfo resetPointInfo : t) {
      v.add(resetPointInfo(resetPointInfo));
    }
    return v;
  }

  static List<com.uber.cadence.entities.PendingActivityInfo> pendingActivityInfoArray(
      List<PendingActivityInfo> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.PendingActivityInfo> v = new ArrayList<>();
    for (PendingActivityInfo pendingActivityInfo : t) {
      v.add(pendingActivityInfo(pendingActivityInfo));
    }
    return v;
  }

  static List<com.uber.cadence.entities.PendingChildExecutionInfo> pendingChildExecutionInfoArray(
      List<PendingChildExecutionInfo> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.PendingChildExecutionInfo> v = new ArrayList<>();
    for (PendingChildExecutionInfo pendingChildExecutionInfo : t) {
      v.add(pendingChildExecutionInfo(pendingChildExecutionInfo));
    }
    return v;
  }

  static Map<String, com.uber.cadence.entities.IndexedValueType> indexedValueTypeMap(
      Map<String, IndexedValueType> t) {
    if (t == null) {
      return null;
    }
    Map<String, com.uber.cadence.entities.IndexedValueType> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, indexedValueType(t.get(key)));
    }
    return v;
  }

  static List<com.uber.cadence.entities.DataBlob> dataBlobArray(List<DataBlob> t) {
    if (t == null || t.size() == 0) {
      return null;
    }
    List<com.uber.cadence.entities.DataBlob> v = new ArrayList<>();
    for (DataBlob dataBlob : t) {
      v.add(dataBlob(dataBlob));
    }
    return v;
  }

  static List<com.uber.cadence.entities.WorkflowExecutionInfo> workflowExecutionInfoArray(
      List<WorkflowExecutionInfo> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.WorkflowExecutionInfo> v = new ArrayList<>();
    for (WorkflowExecutionInfo workflowExecutionInfo : t) {
      v.add(workflowExecutionInfo(workflowExecutionInfo));
    }
    return v;
  }

  static List<com.uber.cadence.entities.DescribeDomainResponse> describeDomainResponseArray(
      List<Domain> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.DescribeDomainResponse> v = new ArrayList<>();
    for (Domain domain : t) {
      v.add(describeDomainResponseDomain(domain));
    }
    return v;
  }

  static List<com.uber.cadence.entities.TaskListPartitionMetadata> taskListPartitionMetadataArray(
      List<TaskListPartitionMetadata> t) {
    if (t == null) {
      return null;
    }
    List<com.uber.cadence.entities.TaskListPartitionMetadata> v = new ArrayList<>();
    for (TaskListPartitionMetadata taskListPartitionMetadata : t) {
      v.add(taskListPartitionMetadata(taskListPartitionMetadata));
    }
    return v;
  }

  static Map<String, com.uber.cadence.entities.WorkflowQuery> workflowQueryMap(
      Map<String, WorkflowQuery> t) {
    if (t == null) {
      return null;
    }
    Map<String, com.uber.cadence.entities.WorkflowQuery> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, workflowQuery(t.get(key)));
    }
    return v;
  }

  static Map<String, com.uber.cadence.entities.ActivityLocalDispatchInfo> activityLocalDispatchInfoMap(
      Map<String, ActivityLocalDispatchInfo> t) {
    if (t == null) {
      return null;
    }
    Map<String, com.uber.cadence.entities.ActivityLocalDispatchInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, activityLocalDispatchInfo(t.get(key)));
    }
    return v;
  }
}
