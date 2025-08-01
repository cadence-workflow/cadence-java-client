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
package com.uber.cadence.internal.compatibility.thrift;

import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.domainStatus;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.encodingType;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.indexedValueType;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.parentClosePolicy;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.pendingActivityState;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.pendingDecisionState;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.taskListKind;
import static com.uber.cadence.internal.compatibility.thrift.EnumMapper.workflowExecutionCloseStatus;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToDays;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.timeToUnixNano;

import com.uber.cadence.ActivityLocalDispatchInfo;
import com.uber.cadence.ActivityType;
import com.uber.cadence.AutoConfigHint;
import com.uber.cadence.BadBinaries;
import com.uber.cadence.BadBinaryInfo;
import com.uber.cadence.ClusterReplicationConfiguration;
import com.uber.cadence.DataBlob;
import com.uber.cadence.DescribeDomainResponse;
import com.uber.cadence.DomainConfiguration;
import com.uber.cadence.DomainInfo;
import com.uber.cadence.DomainReplicationConfiguration;
import com.uber.cadence.Header;
import com.uber.cadence.IndexedValueType;
import com.uber.cadence.Memo;
import com.uber.cadence.PendingActivityInfo;
import com.uber.cadence.PendingChildExecutionInfo;
import com.uber.cadence.PendingDecisionInfo;
import com.uber.cadence.PollerInfo;
import com.uber.cadence.QueryRejected;
import com.uber.cadence.ResetPointInfo;
import com.uber.cadence.ResetPoints;
import com.uber.cadence.RetryPolicy;
import com.uber.cadence.SearchAttributes;
import com.uber.cadence.SupportedClientVersions;
import com.uber.cadence.TaskIDBlock;
import com.uber.cadence.TaskList;
import com.uber.cadence.TaskListMetadata;
import com.uber.cadence.TaskListPartitionMetadata;
import com.uber.cadence.TaskListStatus;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowExecutionConfiguration;
import com.uber.cadence.WorkflowExecutionInfo;
import com.uber.cadence.WorkflowQuery;
import com.uber.cadence.WorkflowType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TypeMapper {

  static byte[] payload(com.uber.cadence.api.v1.Payload t) {
    if (t == null || t == com.uber.cadence.api.v1.Payload.getDefaultInstance()) {
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

  static String failureReason(com.uber.cadence.api.v1.Failure t) {
    if (t == null || t == com.uber.cadence.api.v1.Failure.getDefaultInstance()) {
      return null;
    }
    return t.getReason();
  }

  static byte[] failureDetails(com.uber.cadence.api.v1.Failure t) {
    if (t == null || t == com.uber.cadence.api.v1.Failure.getDefaultInstance()) {
      return null;
    }
    return byteStringToArray(t.getDetails());
  }

  static WorkflowExecution workflowExecution(com.uber.cadence.api.v1.WorkflowExecution t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    WorkflowExecution we = new WorkflowExecution();
    we.setWorkflowId(t.getWorkflowId());
    we.setRunId(t.getRunId());
    return we;
  }

  static String workflowId(com.uber.cadence.api.v1.WorkflowExecution t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    return t.getWorkflowId();
  }

  static String runId(com.uber.cadence.api.v1.WorkflowExecution t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowExecution.getDefaultInstance()) {
      return null;
    }
    return t.getRunId();
  }

  static ActivityType activityType(com.uber.cadence.api.v1.ActivityType t) {
    if (t == null || t == com.uber.cadence.api.v1.ActivityType.getDefaultInstance()) {
      return null;
    }
    ActivityType activityType = new ActivityType();
    activityType.setName(t.getName());
    return activityType;
  }

  static WorkflowType workflowType(com.uber.cadence.api.v1.WorkflowType t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowType.getDefaultInstance()) {
      return null;
    }
    WorkflowType wt = new WorkflowType();
    wt.setName(t.getName());

    return wt;
  }

  static TaskList taskList(com.uber.cadence.api.v1.TaskList t) {
    if (t == null || t == com.uber.cadence.api.v1.TaskList.getDefaultInstance()) {
      return null;
    }
    TaskList taskList = new TaskList();
    taskList.setName(t.getName());
    taskList.setKind(taskListKind(t.getKind()));
    return taskList;
  }

  static RetryPolicy retryPolicy(com.uber.cadence.api.v1.RetryPolicy t) {
    if (t == null || t == com.uber.cadence.api.v1.RetryPolicy.getDefaultInstance()) {
      return null;
    }
    RetryPolicy res = new RetryPolicy();
    res.setInitialIntervalInSeconds(durationToSeconds(t.getInitialInterval()));
    res.setBackoffCoefficient(t.getBackoffCoefficient());
    res.setMaximumIntervalInSeconds(durationToSeconds(t.getMaximumInterval()));
    res.setMaximumAttempts(t.getMaximumAttempts());
    res.setNonRetriableErrorReasons(t.getNonRetryableErrorReasonsList());
    res.setExpirationIntervalInSeconds(durationToSeconds(t.getExpirationInterval()));
    return res;
  }

  static Header header(com.uber.cadence.api.v1.Header t) {
    if (t == null || t == com.uber.cadence.api.v1.Header.getDefaultInstance()) {
      return null;
    }
    Header res = new Header();
    res.setFields(payloadMap(t.getFieldsMap()));
    return res;
  }

  static Memo memo(com.uber.cadence.api.v1.Memo t) {
    if (t == null || t == com.uber.cadence.api.v1.Memo.getDefaultInstance()) {
      return null;
    }
    Memo res = new Memo();
    res.setFields(payloadMap(t.getFieldsMap()));
    return res;
  }

  static SearchAttributes searchAttributes(com.uber.cadence.api.v1.SearchAttributes t) {
    if (t == null
        || t.getAllFields().size() == 0
        || t == com.uber.cadence.api.v1.SearchAttributes.getDefaultInstance()) {
      return null;
    }
    SearchAttributes res = new SearchAttributes();
    res.setIndexedFields(payloadMap(t.getIndexedFieldsMap()));
    return res;
  }

  static BadBinaries badBinaries(com.uber.cadence.api.v1.BadBinaries t) {
    if (t == null || t == com.uber.cadence.api.v1.BadBinaries.getDefaultInstance()) {
      return null;
    }
    BadBinaries badBinaries = new BadBinaries();
    badBinaries.setBinaries(badBinaryInfoMap(t.getBinariesMap()));
    return badBinaries;
  }

  static BadBinaryInfo badBinaryInfo(com.uber.cadence.api.v1.BadBinaryInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.BadBinaryInfo.getDefaultInstance()) {
      return null;
    }
    BadBinaryInfo res = new BadBinaryInfo();
    res.setReason(t.getReason());
    res.setOperator(t.getOperator());
    res.setCreatedTimeNano(timeToUnixNano(t.getCreatedTime()));
    return res;
  }

  static Map<String, BadBinaryInfo> badBinaryInfoMap(
      Map<String, com.uber.cadence.api.v1.BadBinaryInfo> t) {
    if (t == null) {
      return null;
    }
    Map<String, BadBinaryInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, badBinaryInfo(t.get(key)));
    }
    return v;
  }

  static WorkflowQuery workflowQuery(com.uber.cadence.api.v1.WorkflowQuery t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowQuery.getDefaultInstance()) {
      return null;
    }
    WorkflowQuery res = new WorkflowQuery();
    res.setQueryType(t.getQueryType());
    res.setQueryArgs(payload(t.getQueryArgs()));
    return res;
  }

  static Map<String, ByteBuffer> payloadMap(Map<String, com.uber.cadence.api.v1.Payload> t) {
    if (t == null) {
      return null;
    }
    Map<String, ByteBuffer> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, ByteBuffer.wrap(payload(t.get(key))));
    }
    return v;
  }

  static List<ClusterReplicationConfiguration> clusterReplicationConfigurationArray(
      List<com.uber.cadence.api.v1.ClusterReplicationConfiguration> t) {
    if (t == null) {
      return null;
    }
    List<ClusterReplicationConfiguration> v = new ArrayList<>();
    for (int i = 0; i < t.size(); i++) {
      v.add(clusterReplicationConfiguration(t.get(i)));
    }
    return v;
  }

  static ClusterReplicationConfiguration clusterReplicationConfiguration(
      com.uber.cadence.api.v1.ClusterReplicationConfiguration t) {
    if (t == null
        || t == com.uber.cadence.api.v1.ClusterReplicationConfiguration.getDefaultInstance()) {
      return null;
    }
    ClusterReplicationConfiguration res = new ClusterReplicationConfiguration();
    res.setClusterName(t.getClusterName());
    return res;
  }

  static DataBlob dataBlob(com.uber.cadence.api.v1.DataBlob t) {
    if (t == null || t == com.uber.cadence.api.v1.DataBlob.getDefaultInstance()) {
      return null;
    }
    DataBlob dataBlob = new DataBlob();
    dataBlob.setEncodingType(encodingType(t.getEncodingType()));
    dataBlob.setData(byteStringToArray(t.getData()));
    return dataBlob;
  }

  static long externalInitiatedId(com.uber.cadence.api.v1.ExternalExecutionInfo t) {
    return t.getInitiatedId();
  }

  static WorkflowExecution externalWorkflowExecution(
      com.uber.cadence.api.v1.ExternalExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ExternalExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return workflowExecution(t.getWorkflowExecution());
  }

  static ResetPoints resetPoints(com.uber.cadence.api.v1.ResetPoints t) {
    if (t == null || t == com.uber.cadence.api.v1.ResetPoints.getDefaultInstance()) {
      return null;
    }
    ResetPoints res = new ResetPoints();
    res.setPoints(resetPointInfoArray(t.getPointsList()));
    return res;
  }

  static ResetPointInfo resetPointInfo(com.uber.cadence.api.v1.ResetPointInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ResetPointInfo.getDefaultInstance()) {
      return null;
    }
    ResetPointInfo res = new ResetPointInfo();
    res.setBinaryChecksum(t.getBinaryChecksum());
    res.setRunId(t.getRunId());
    res.setFirstDecisionCompletedId(t.getFirstDecisionCompletedId());
    res.setCreatedTimeNano(timeToUnixNano(t.getCreatedTime()));
    res.setExpiringTimeNano(timeToUnixNano(t.getExpiringTime()));
    res.setResettable(t.getResettable());
    return res;
  }

  static PollerInfo pollerInfo(com.uber.cadence.api.v1.PollerInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.PollerInfo.getDefaultInstance()) {
      return null;
    }
    PollerInfo res = new PollerInfo();
    res.setLastAccessTime(timeToUnixNano(t.getLastAccessTime()));
    res.setIdentity(t.getIdentity());
    res.setRatePerSecond(t.getRatePerSecond());
    return res;
  }

  static TaskListStatus taskListStatus(com.uber.cadence.api.v1.TaskListStatus t) {
    if (t == null || t == com.uber.cadence.api.v1.TaskListStatus.getDefaultInstance()) {
      return null;
    }
    TaskListStatus res = new TaskListStatus();
    res.setBacklogCountHint(t.getBacklogCountHint());
    res.setReadLevel(t.getReadLevel());
    res.setAckLevel(t.getAckLevel());
    res.setRatePerSecond(t.getRatePerSecond());
    res.setTaskIDBlock(taskIdBlock(t.getTaskIdBlock()));
    return res;
  }

  static TaskIDBlock taskIdBlock(com.uber.cadence.api.v1.TaskIDBlock t) {
    if (t == null || t == com.uber.cadence.api.v1.TaskIDBlock.getDefaultInstance()) {
      return null;
    }
    TaskIDBlock res = new TaskIDBlock();
    res.setStartID(t.getStartId());
    res.setEndID(t.getEndId());
    return res;
  }

  static WorkflowExecutionConfiguration workflowExecutionConfiguration(
      com.uber.cadence.api.v1.WorkflowExecutionConfiguration t) {
    if (t == null
        || t == com.uber.cadence.api.v1.WorkflowExecutionConfiguration.getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionConfiguration res = new WorkflowExecutionConfiguration();
    res.setTaskList(taskList(t.getTaskList()));
    res.setExecutionStartToCloseTimeoutSeconds(
        durationToSeconds(t.getExecutionStartToCloseTimeout()));
    res.setTaskStartToCloseTimeoutSeconds(durationToSeconds(t.getTaskStartToCloseTimeout()));
    return res;
  }

  static WorkflowExecutionInfo workflowExecutionInfo(
      com.uber.cadence.api.v1.WorkflowExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.WorkflowExecutionInfo.getDefaultInstance()) {
      return null;
    }
    WorkflowExecutionInfo res = new WorkflowExecutionInfo();
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

  static String parentDomainId(com.uber.cadence.api.v1.ParentExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return t.getDomainId();
  }

  static String parentDomainName(com.uber.cadence.api.v1.ParentExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return t.getDomainName();
  }

  static long parentInitiatedId(com.uber.cadence.api.v1.ParentExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ParentExecutionInfo.getDefaultInstance()) {
      return -1;
    }
    return t.getInitiatedId();
  }

  static WorkflowExecution parentWorkflowExecution(com.uber.cadence.api.v1.ParentExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ParentExecutionInfo.getDefaultInstance()) {
      return null;
    }
    return workflowExecution(t.getWorkflowExecution());
  }

  static PendingActivityInfo pendingActivityInfo(com.uber.cadence.api.v1.PendingActivityInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.PendingActivityInfo.getDefaultInstance()) {
      return null;
    }
    PendingActivityInfo res = new PendingActivityInfo();
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

  static PendingChildExecutionInfo pendingChildExecutionInfo(
      com.uber.cadence.api.v1.PendingChildExecutionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.PendingChildExecutionInfo.getDefaultInstance()) {
      return null;
    }
    PendingChildExecutionInfo res = new PendingChildExecutionInfo();
    res.setWorkflowID(workflowId(t.getWorkflowExecution()));
    res.setRunID(runId(t.getWorkflowExecution()));
    res.setWorkflowTypName(t.getWorkflowTypeName());
    res.setInitiatedID(t.getInitiatedId());
    res.setParentClosePolicy(parentClosePolicy(t.getParentClosePolicy()));
    return res;
  }

  static PendingDecisionInfo pendingDecisionInfo(com.uber.cadence.api.v1.PendingDecisionInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.PendingDecisionInfo.getDefaultInstance()) {
      return null;
    }
    PendingDecisionInfo res = new PendingDecisionInfo();
    res.setState(pendingDecisionState(t.getState()));
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setAttempt(t.getAttempt());
    res.setOriginalScheduledTimestamp(timeToUnixNano(t.getOriginalScheduledTime()));
    return res;
  }

  static ActivityLocalDispatchInfo activityLocalDispatchInfo(
      com.uber.cadence.api.v1.ActivityLocalDispatchInfo t) {
    if (t == null || t == com.uber.cadence.api.v1.ActivityLocalDispatchInfo.getDefaultInstance()) {
      return null;
    }
    ActivityLocalDispatchInfo res = new ActivityLocalDispatchInfo();
    res.setActivityId(t.getActivityId());
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setScheduledTimestampOfThisAttempt(timeToUnixNano(t.getScheduledTimeOfThisAttempt()));
    res.setTaskToken(byteStringToArray(t.getTaskToken()));
    return res;
  }

  static SupportedClientVersions supportedClientVersions(
      com.uber.cadence.api.v1.SupportedClientVersions t) {
    if (t == null || t == com.uber.cadence.api.v1.SupportedClientVersions.getDefaultInstance()) {
      return null;
    }
    SupportedClientVersions res = new SupportedClientVersions();
    res.setGoSdk(t.getGoSdk());
    res.setJavaSdk(t.getJavaSdk());
    return res;
  }

  static DescribeDomainResponse describeDomainResponseDomain(com.uber.cadence.api.v1.Domain t) {
    if (t == null || t == com.uber.cadence.api.v1.Domain.getDefaultInstance()) {
      return null;
    }
    DescribeDomainResponse res = new DescribeDomainResponse();
    DomainInfo domainInfo = new DomainInfo();
    res.setDomainInfo(domainInfo);

    domainInfo.setName(t.getName());
    domainInfo.setStatus(domainStatus(t.getStatus()));
    domainInfo.setDescription(t.getDescription());
    domainInfo.setOwnerEmail(t.getOwnerEmail());
    domainInfo.setData(t.getDataMap());
    domainInfo.setUuid(t.getId());

    DomainConfiguration domainConfiguration = new DomainConfiguration();
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

    DomainReplicationConfiguration domainReplicationConfiguration =
        new DomainReplicationConfiguration();
    res.setReplicationConfiguration(domainReplicationConfiguration);

    domainReplicationConfiguration.setActiveClusterName(t.getActiveClusterName());
    domainReplicationConfiguration.setClusters(
        clusterReplicationConfigurationArray(t.getClustersList()));
    res.setFailoverVersion(t.getFailoverVersion());
    res.setIsGlobalDomain(t.getIsGlobalDomain());

    return res;
  }

  static TaskListMetadata taskListMetadata(com.uber.cadence.api.v1.TaskListMetadata t) {
    if (t == null) {
      return null;
    }
    TaskListMetadata res = new TaskListMetadata();
    res.setMaxTasksPerSecond(t.getMaxTasksPerSecond().getValue());
    return res;
  }

  static TaskListPartitionMetadata taskListPartitionMetadata(
      com.uber.cadence.api.v1.TaskListPartitionMetadata t) {
    if (t == null || t == com.uber.cadence.api.v1.TaskListPartitionMetadata.getDefaultInstance()) {
      return null;
    }
    TaskListPartitionMetadata res = new TaskListPartitionMetadata();
    res.setKey(t.getKey());
    res.setOwnerHostName(t.getOwnerHostName());
    return res;
  }

  static QueryRejected queryRejected(com.uber.cadence.api.v1.QueryRejected t) {
    if (t == null || t == com.uber.cadence.api.v1.QueryRejected.getDefaultInstance()) {
      return null;
    }
    QueryRejected res = new QueryRejected();
    res.setCloseStatus(workflowExecutionCloseStatus(t.getCloseStatus()));
    return res;
  }

  static List<PollerInfo> pollerInfoArray(List<com.uber.cadence.api.v1.PollerInfo> t) {
    if (t == null) {
      return null;
    }
    List<PollerInfo> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.PollerInfo pollerInfo : t) {
      v.add(pollerInfo(pollerInfo));
    }
    return v;
  }

  static List<ResetPointInfo> resetPointInfoArray(List<com.uber.cadence.api.v1.ResetPointInfo> t) {
    if (t == null) {
      return null;
    }
    List<ResetPointInfo> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.ResetPointInfo resetPointInfo : t) {
      v.add(resetPointInfo(resetPointInfo));
    }
    return v;
  }

  static List<PendingActivityInfo> pendingActivityInfoArray(
      List<com.uber.cadence.api.v1.PendingActivityInfo> t) {
    if (t == null) {
      return null;
    }
    List<PendingActivityInfo> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.PendingActivityInfo pendingActivityInfo : t) {
      v.add(pendingActivityInfo(pendingActivityInfo));
    }
    return v;
  }

  static List<PendingChildExecutionInfo> pendingChildExecutionInfoArray(
      List<com.uber.cadence.api.v1.PendingChildExecutionInfo> t) {
    if (t == null) {
      return null;
    }
    List<PendingChildExecutionInfo> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.PendingChildExecutionInfo pendingChildExecutionInfo : t) {
      v.add(pendingChildExecutionInfo(pendingChildExecutionInfo));
    }
    return v;
  }

  static Map<String, IndexedValueType> indexedValueTypeMap(
      Map<String, com.uber.cadence.api.v1.IndexedValueType> t) {
    if (t == null) {
      return null;
    }
    Map<String, IndexedValueType> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, indexedValueType(t.get(key)));
    }
    return v;
  }

  static List<DataBlob> dataBlobArray(List<com.uber.cadence.api.v1.DataBlob> t) {
    if (t == null || t.size() == 0) {
      return null;
    }
    List<DataBlob> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.DataBlob dataBlob : t) {
      v.add(dataBlob(dataBlob));
    }
    return v;
  }

  static List<WorkflowExecutionInfo> workflowExecutionInfoArray(
      List<com.uber.cadence.api.v1.WorkflowExecutionInfo> t) {
    if (t == null) {
      return null;
    }
    List<WorkflowExecutionInfo> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.WorkflowExecutionInfo workflowExecutionInfo : t) {
      v.add(workflowExecutionInfo(workflowExecutionInfo));
    }
    return v;
  }

  static List<DescribeDomainResponse> describeDomainResponseArray(
      List<com.uber.cadence.api.v1.Domain> t) {
    if (t == null) {
      return null;
    }
    List<DescribeDomainResponse> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.Domain domain : t) {
      v.add(describeDomainResponseDomain(domain));
    }
    return v;
  }

  static List<TaskListPartitionMetadata> taskListPartitionMetadataArray(
      List<com.uber.cadence.api.v1.TaskListPartitionMetadata> t) {
    if (t == null) {
      return null;
    }
    List<TaskListPartitionMetadata> v = new ArrayList<>();
    for (com.uber.cadence.api.v1.TaskListPartitionMetadata taskListPartitionMetadata : t) {
      v.add(taskListPartitionMetadata(taskListPartitionMetadata));
    }
    return v;
  }

  static Map<String, WorkflowQuery> workflowQueryMap(
      Map<String, com.uber.cadence.api.v1.WorkflowQuery> t) {
    if (t == null) {
      return null;
    }
    Map<String, WorkflowQuery> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, workflowQuery(t.get(key)));
    }
    return v;
  }

  static Map<String, ActivityLocalDispatchInfo> activityLocalDispatchInfoMap(
      Map<String, com.uber.cadence.api.v1.ActivityLocalDispatchInfo> t) {
    if (t == null) {
      return null;
    }
    Map<String, ActivityLocalDispatchInfo> v = new HashMap<>();
    for (String key : t.keySet()) {
      v.put(key, activityLocalDispatchInfo(t.get(key)));
    }
    return v;
  }

  static AutoConfigHint autoConfigHint(com.uber.cadence.api.v1.AutoConfigHint t) {
    if (t == null) {
      return null;
    }
    AutoConfigHint autoConfigHint = new AutoConfigHint();
    autoConfigHint.setEnableAutoConfig(t.getEnableAutoConfig());
    autoConfigHint.setPollerWaitTimeInMs(t.getPollerWaitTimeInMs());
    return autoConfigHint;
  }
}
