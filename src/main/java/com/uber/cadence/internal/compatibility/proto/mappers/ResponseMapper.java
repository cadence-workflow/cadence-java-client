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
package com.uber.cadence.internal.compatibility.proto.mappers;

import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.domainStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.durationToDays;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.timeToUnixNano;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.toInt64Value;
import static com.uber.cadence.internal.compatibility.proto.mappers.HistoryMapper.history;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.activityLocalDispatchInfoMap;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.badBinaries;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.clusterReplicationConfigurationArrayFromProto;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.dataBlobArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.describeDomainResponseArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.indexedValueTypeMap;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.pendingActivityInfoArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.pendingChildExecutionInfoArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.pendingDecisionInfo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.pollerInfoArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.queryRejected;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.supportedClientVersions;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskListPartitionMetadataArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskListStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecutionConfiguration;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecutionInfo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecutionInfoArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowQuery;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowQueryMap;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowType;

import com.uber.cadence.api.v1.*;
import java.util.Map;
import java.util.stream.Collectors;

public class ResponseMapper {

  public static com.uber.cadence.StartWorkflowExecutionResponse startWorkflowExecutionResponse(
      StartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new com.uber.cadence.StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static com.uber.cadence.StartWorkflowExecutionAsyncResponse
      startWorkflowExecutionAsyncResponse(StartWorkflowExecutionAsyncResponse t) {
    return t == null ? null : new com.uber.cadence.StartWorkflowExecutionAsyncResponse();
  }

  public static com.uber.cadence.DescribeTaskListResponse describeTaskListResponse(
      DescribeTaskListResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.DescribeTaskListResponse describeTaskListResponse =
        new com.uber.cadence.DescribeTaskListResponse();
    describeTaskListResponse.setPollers(pollerInfoArray(t.getPollersList()));
    describeTaskListResponse.setTaskListStatus(taskListStatus(t.getTaskListStatus()));
    return describeTaskListResponse;
  }

  public static com.uber.cadence.RestartWorkflowExecutionResponse restartWorkflowExecutionResponse(
      RestartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.RestartWorkflowExecutionResponse restartWorkflowExecutionResponse =
        new com.uber.cadence.RestartWorkflowExecutionResponse();
    restartWorkflowExecutionResponse.setRunId(t.getRunId());
    return restartWorkflowExecutionResponse;
  }

  public static com.uber.cadence.DescribeWorkflowExecutionResponse
      describeWorkflowExecutionResponse(DescribeWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new com.uber.cadence.DescribeWorkflowExecutionResponse();
    describeWorkflowExecutionResponse.setExecutionConfiguration(
        workflowExecutionConfiguration(t.getExecutionConfiguration()));
    describeWorkflowExecutionResponse.setWorkflowExecutionInfo(
        workflowExecutionInfo(t.getWorkflowExecutionInfo()));
    describeWorkflowExecutionResponse.setPendingActivities(
        pendingActivityInfoArray(t.getPendingActivitiesList()));
    describeWorkflowExecutionResponse.setPendingChildren(
        pendingChildExecutionInfoArray(t.getPendingChildrenList()));
    describeWorkflowExecutionResponse.setPendingDecision(
        pendingDecisionInfo(t.getPendingDecision()));
    return describeWorkflowExecutionResponse;
  }

  public static com.uber.cadence.ClusterInfo getClusterInfoResponse(GetClusterInfoResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ClusterInfo clusterInfo = new com.uber.cadence.ClusterInfo();
    clusterInfo.setSupportedClientVersions(supportedClientVersions(t.getSupportedClientVersions()));
    return clusterInfo;
  }

  public static com.uber.cadence.GetSearchAttributesResponse getSearchAttributesResponse(
      GetSearchAttributesResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.GetSearchAttributesResponse getSearchAttributesResponse =
        new com.uber.cadence.GetSearchAttributesResponse();
    getSearchAttributesResponse.setKeys(indexedValueTypeMap(t.getKeysMap()));
    return getSearchAttributesResponse;
  }

  public static com.uber.cadence.GetWorkflowExecutionHistoryResponse
      getWorkflowExecutionHistoryResponse(GetWorkflowExecutionHistoryResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistoryResponse =
        new com.uber.cadence.GetWorkflowExecutionHistoryResponse();
    getWorkflowExecutionHistoryResponse.setHistory(history(t.getHistory()));
    getWorkflowExecutionHistoryResponse.setRawHistory(dataBlobArray(t.getRawHistoryList()));
    getWorkflowExecutionHistoryResponse.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    getWorkflowExecutionHistoryResponse.setArchived(t.getArchived());
    return getWorkflowExecutionHistoryResponse;
  }

  public static com.uber.cadence.ListArchivedWorkflowExecutionsResponse
      listArchivedWorkflowExecutionsResponse(ListArchivedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListArchivedWorkflowExecutionsResponse res =
        new com.uber.cadence.ListArchivedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.ListClosedWorkflowExecutionsResponse
      listClosedWorkflowExecutionsResponse(ListClosedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListClosedWorkflowExecutionsResponse res =
        new com.uber.cadence.ListClosedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.ListOpenWorkflowExecutionsResponse
      listOpenWorkflowExecutionsResponse(ListOpenWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListOpenWorkflowExecutionsResponse res =
        new com.uber.cadence.ListOpenWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.ListTaskListPartitionsResponse listTaskListPartitionsResponse(
      ListTaskListPartitionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListTaskListPartitionsResponse res =
        new com.uber.cadence.ListTaskListPartitionsResponse();
    res.setActivityTaskListPartitions(
        taskListPartitionMetadataArray(t.getActivityTaskListPartitionsList()));
    res.setDecisionTaskListPartitions(
        taskListPartitionMetadataArray(t.getDecisionTaskListPartitionsList()));
    return res;
  }

  public static com.uber.cadence.ListWorkflowExecutionsResponse listWorkflowExecutionsResponse(
      ListWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListWorkflowExecutionsResponse res =
        new com.uber.cadence.ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.PollForActivityTaskResponse pollForActivityTaskResponse(
      PollForActivityTaskResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.PollForActivityTaskResponse res =
        new com.uber.cadence.PollForActivityTaskResponse();
    res.setTaskToken(byteStringToArray(t.getTaskToken()));
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setActivityId(t.getActivityId());
    res.setActivityType(activityType(t.getActivityType()));
    res.setInput(payload(t.getInput()));
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setScheduleToCloseTimeoutSeconds(durationToSeconds(t.getScheduleToCloseTimeout()));
    res.setStartToCloseTimeoutSeconds(durationToSeconds(t.getStartToCloseTimeout()));
    res.setHeartbeatTimeoutSeconds(durationToSeconds(t.getHeartbeatTimeout()));
    res.setAttempt(t.getAttempt());
    res.setScheduledTimestampOfThisAttempt(timeToUnixNano(t.getScheduledTimeOfThisAttempt()));
    res.setHeartbeatDetails(payload(t.getHeartbeatDetails()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setWorkflowDomain(t.getWorkflowDomain());
    res.setHeader(header(t.getHeader()));
    return res;
  }

  public static com.uber.cadence.PollForDecisionTaskResponse pollForDecisionTaskResponse(
      PollForDecisionTaskResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.PollForDecisionTaskResponse res =
        new com.uber.cadence.PollForDecisionTaskResponse();
    res.setTaskToken(byteStringToArray(t.getTaskToken()));
    res.setWorkflowExecution(workflowExecution(t.getWorkflowExecution()));
    res.setWorkflowType(workflowType(t.getWorkflowType()));
    res.setPreviousStartedEventId(toInt64Value(t.getPreviousStartedEventId()));
    res.setStartedEventId(t.getStartedEventId());
    res.setAttempt(t.getAttempt());
    res.setBacklogCountHint(t.getBacklogCountHint());
    res.setHistory(history(t.getHistory()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    if (t.getQuery() != WorkflowQuery.getDefaultInstance()) {
      res.setQuery(workflowQuery(t.getQuery()));
    }
    res.setWorkflowExecutionTaskList(taskList(t.getWorkflowExecutionTaskList()));
    res.setScheduledTimestamp(timeToUnixNano(t.getScheduledTime()));
    res.setStartedTimestamp(timeToUnixNano(t.getStartedTime()));
    res.setQueries(workflowQueryMap(t.getQueriesMap()));
    res.setNextEventId(t.getNextEventId());
    return res;
  }

  public static com.uber.cadence.QueryWorkflowResponse queryWorkflowResponse(
      QueryWorkflowResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.QueryWorkflowResponse res = new com.uber.cadence.QueryWorkflowResponse();
    res.setQueryResult(payload(t.getQueryResult()));
    res.setQueryRejected(queryRejected(t.getQueryRejected()));
    return res;
  }

  public static com.uber.cadence.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatByIdResponse(RecordActivityTaskHeartbeatByIDResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatResponse(RecordActivityTaskHeartbeatResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.ResetWorkflowExecutionResponse resetWorkflowExecutionResponse(
      ResetWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ResetWorkflowExecutionResponse res =
        new com.uber.cadence.ResetWorkflowExecutionResponse();
    res.setRunId(t.getRunId());
    return res;
  }

  public static com.uber.cadence.RespondDecisionTaskCompletedResponse
      respondDecisionTaskCompletedResponse(RespondDecisionTaskCompletedResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.RespondDecisionTaskCompletedResponse res =
        new com.uber.cadence.RespondDecisionTaskCompletedResponse();
    res.setDecisionTask(pollForDecisionTaskResponse(t.getDecisionTask()));
    res.setActivitiesToDispatchLocally(
        activityLocalDispatchInfoMap(t.getActivitiesToDispatchLocallyMap()));
    return res;
  }

  public static com.uber.cadence.ListWorkflowExecutionsResponse scanWorkflowExecutionsResponse(
      ScanWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListWorkflowExecutionsResponse res =
        new com.uber.cadence.ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.CountWorkflowExecutionsResponse countWorkflowExecutionsResponse(
      CountWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.CountWorkflowExecutionsResponse res =
        new com.uber.cadence.CountWorkflowExecutionsResponse();
    res.setCount(t.getCount());
    return res;
  }

  public static com.uber.cadence.DescribeDomainResponse describeDomainResponse(
      DescribeDomainResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.DescribeDomainResponse response =
        new com.uber.cadence.DescribeDomainResponse();
    com.uber.cadence.DomainInfo domainInfo = new com.uber.cadence.DomainInfo();
    response.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    com.uber.cadence.DomainConfiguration domainConfiguration =
        new com.uber.cadence.DomainConfiguration();
    response.setConfiguration(domainConfiguration);

    domainConfiguration.setWorkflowExecutionRetentionPeriodInDays(
        durationToDays(t.getDomain().getWorkflowExecutionRetentionPeriod()));
    domainConfiguration.setEmitMetric(true);
    domainConfiguration.setBadBinaries(badBinaries(t.getDomain().getBadBinaries()));
    domainConfiguration.setHistoryArchivalStatus(
        archivalStatus(t.getDomain().getHistoryArchivalStatus()));
    domainConfiguration.setHistoryArchivalURI(t.getDomain().getHistoryArchivalUri());
    domainConfiguration.setVisibilityArchivalStatus(
        archivalStatus(t.getDomain().getVisibilityArchivalStatus()));
    domainConfiguration.setVisibilityArchivalURI(t.getDomain().getVisibilityArchivalUri());

    com.uber.cadence.DomainReplicationConfiguration replicationConfiguration =
        new com.uber.cadence.DomainReplicationConfiguration();
    response.setReplicationConfiguration(replicationConfiguration);

    replicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    replicationConfiguration.setClusters(
        clusterReplicationConfigurationArrayFromProto(t.getDomain().getClustersList()));

    response.setFailoverVersion(t.getDomain().getFailoverVersion());
    response.setGlobalDomain(t.getDomain().getIsGlobalDomain());
    return response;
  }

  public static com.uber.cadence.ListDomainsResponse listDomainsResponse(ListDomainsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.ListDomainsResponse res = new com.uber.cadence.ListDomainsResponse();
    res.setDomains(describeDomainResponseArray(t.getDomainsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.StartWorkflowExecutionResponse
      signalWithStartWorkflowExecutionResponse(SignalWithStartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new com.uber.cadence.StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse
      signalWithStartWorkflowExecutionAsyncResponse(
          SignalWithStartWorkflowExecutionAsyncResponse t) {
    return t == null ? null : new com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse();
  }

  public static com.uber.cadence.UpdateDomainResponse updateDomainResponse(UpdateDomainResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.UpdateDomainResponse updateDomainResponse =
        new com.uber.cadence.UpdateDomainResponse();
    com.uber.cadence.DomainInfo domainInfo = new com.uber.cadence.DomainInfo();
    updateDomainResponse.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    com.uber.cadence.DomainConfiguration domainConfiguration =
        new com.uber.cadence.DomainConfiguration();
    updateDomainResponse.setConfiguration(domainConfiguration);

    domainConfiguration.setWorkflowExecutionRetentionPeriodInDays(
        durationToDays(t.getDomain().getWorkflowExecutionRetentionPeriod()));
    domainConfiguration.setEmitMetric(true);
    domainConfiguration.setBadBinaries(badBinaries(t.getDomain().getBadBinaries()));
    domainConfiguration.setHistoryArchivalStatus(
        archivalStatus(t.getDomain().getHistoryArchivalStatus()));
    domainConfiguration.setHistoryArchivalURI(t.getDomain().getHistoryArchivalUri());
    domainConfiguration.setVisibilityArchivalStatus(
        archivalStatus(t.getDomain().getVisibilityArchivalStatus()));
    domainConfiguration.setVisibilityArchivalURI(t.getDomain().getVisibilityArchivalUri());

    com.uber.cadence.DomainReplicationConfiguration domainReplicationConfiguration =
        new com.uber.cadence.DomainReplicationConfiguration();
    updateDomainResponse.setReplicationConfiguration(domainReplicationConfiguration);

    domainReplicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    domainReplicationConfiguration.setClusters(
        clusterReplicationConfigurationArrayFromProto(t.getDomain().getClustersList()));
    updateDomainResponse.setFailoverVersion(t.getDomain().getFailoverVersion());
    updateDomainResponse.setGlobalDomain(t.getDomain().getIsGlobalDomain());
    return updateDomainResponse;
  }

  public static com.uber.cadence.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatResponse(
          RecordActivityTaskHeartbeatByIDResponse recordActivityTaskHeartbeatByID) {
    if (recordActivityTaskHeartbeatByID == null) {
      return null;
    }
    com.uber.cadence.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(recordActivityTaskHeartbeatByID.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.ResetStickyTaskListResponse resetStickyTaskListResponse(
      ResetStickyTaskListResponse resetStickyTaskList) {
    if (resetStickyTaskList == null) {
      return null;
    }
    com.uber.cadence.ResetStickyTaskListResponse res =
        new com.uber.cadence.ResetStickyTaskListResponse();
    return res;
  }

  public static com.uber.cadence.ClusterInfo clusterInfoResponse(
      GetClusterInfoResponse clusterInfo) {
    if (clusterInfo == null) {
      return null;
    }
    com.uber.cadence.ClusterInfo res = new com.uber.cadence.ClusterInfo();
    res.setSupportedClientVersions(
        TypeMapper.supportedClientVersions(clusterInfo.getSupportedClientVersions()));
    return res;
  }

  public static com.uber.cadence.GetTaskListsByDomainResponse getTaskListsByDomainResponse(
      GetTaskListsByDomainResponse taskListsByDomain) {
    if (taskListsByDomain == null) {
      return null;
    }
    com.uber.cadence.GetTaskListsByDomainResponse res =
        new com.uber.cadence.GetTaskListsByDomainResponse();

    res.setActivityTaskListMap(
        taskListsByDomain
            .getActivityTaskListMapMap()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> describeTaskListResponse(e.getValue()))));
    res.setDecisionTaskListMap(
        taskListsByDomain
            .getDecisionTaskListMapMap()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> describeTaskListResponse(e.getValue()))));
    return res;
  }
}
