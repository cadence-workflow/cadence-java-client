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

  public static com.uber.cadence.entities.StartWorkflowExecutionResponse
      startWorkflowExecutionResponse(StartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new com.uber.cadence.entities.StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static com.uber.cadence.entities.StartWorkflowExecutionAsyncResponse
      startWorkflowExecutionAsyncResponse(StartWorkflowExecutionAsyncResponse t) {
    return t == null ? null : new com.uber.cadence.entities.StartWorkflowExecutionAsyncResponse();
  }

  public static com.uber.cadence.entities.DescribeTaskListResponse describeTaskListResponse(
      DescribeTaskListResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.DescribeTaskListResponse describeTaskListResponse =
        new com.uber.cadence.entities.DescribeTaskListResponse();
    describeTaskListResponse.setPollers(pollerInfoArray(t.getPollersList()));
    describeTaskListResponse.setTaskListStatus(taskListStatus(t.getTaskListStatus()));
    return describeTaskListResponse;
  }

  public static com.uber.cadence.entities.RestartWorkflowExecutionResponse
      restartWorkflowExecutionResponse(RestartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.RestartWorkflowExecutionResponse restartWorkflowExecutionResponse =
        new com.uber.cadence.entities.RestartWorkflowExecutionResponse();
    restartWorkflowExecutionResponse.setRunId(t.getRunId());
    return restartWorkflowExecutionResponse;
  }

  public static com.uber.cadence.entities.DescribeWorkflowExecutionResponse
      describeWorkflowExecutionResponse(DescribeWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new com.uber.cadence.entities.DescribeWorkflowExecutionResponse();
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

  public static com.uber.cadence.entities.ClusterInfo getClusterInfoResponse(
      GetClusterInfoResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ClusterInfo clusterInfo = new com.uber.cadence.entities.ClusterInfo();
    clusterInfo.setSupportedClientVersions(supportedClientVersions(t.getSupportedClientVersions()));
    return clusterInfo;
  }

  public static com.uber.cadence.entities.GetSearchAttributesResponse getSearchAttributesResponse(
      GetSearchAttributesResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.GetSearchAttributesResponse getSearchAttributesResponse =
        new com.uber.cadence.entities.GetSearchAttributesResponse();
    getSearchAttributesResponse.setKeys(indexedValueTypeMap(t.getKeysMap()));
    return getSearchAttributesResponse;
  }

  public static com.uber.cadence.entities.GetWorkflowExecutionHistoryResponse
      getWorkflowExecutionHistoryResponse(GetWorkflowExecutionHistoryResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.GetWorkflowExecutionHistoryResponse
        getWorkflowExecutionHistoryResponse =
            new com.uber.cadence.entities.GetWorkflowExecutionHistoryResponse();
    getWorkflowExecutionHistoryResponse.setHistory(history(t.getHistory()));
    getWorkflowExecutionHistoryResponse.setRawHistory(dataBlobArray(t.getRawHistoryList()));
    getWorkflowExecutionHistoryResponse.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    getWorkflowExecutionHistoryResponse.setArchived(t.getArchived());
    return getWorkflowExecutionHistoryResponse;
  }

  public static com.uber.cadence.entities.ListArchivedWorkflowExecutionsResponse
      listArchivedWorkflowExecutionsResponse(ListArchivedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListArchivedWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.ListArchivedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.ListClosedWorkflowExecutionsResponse
      listClosedWorkflowExecutionsResponse(ListClosedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListClosedWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.ListClosedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.ListOpenWorkflowExecutionsResponse
      listOpenWorkflowExecutionsResponse(ListOpenWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListOpenWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.ListOpenWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.ListTaskListPartitionsResponse
      listTaskListPartitionsResponse(ListTaskListPartitionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListTaskListPartitionsResponse res =
        new com.uber.cadence.entities.ListTaskListPartitionsResponse();
    res.setActivityTaskListPartitions(
        taskListPartitionMetadataArray(t.getActivityTaskListPartitionsList()));
    res.setDecisionTaskListPartitions(
        taskListPartitionMetadataArray(t.getDecisionTaskListPartitionsList()));
    return res;
  }

  public static com.uber.cadence.entities.ListWorkflowExecutionsResponse
      listWorkflowExecutionsResponse(ListWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.PollForActivityTaskResponse pollForActivityTaskResponse(
      PollForActivityTaskResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.PollForActivityTaskResponse res =
        new com.uber.cadence.entities.PollForActivityTaskResponse();
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

  public static com.uber.cadence.entities.PollForDecisionTaskResponse pollForDecisionTaskResponse(
      PollForDecisionTaskResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.PollForDecisionTaskResponse res =
        new com.uber.cadence.entities.PollForDecisionTaskResponse();
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

  public static com.uber.cadence.entities.QueryWorkflowResponse queryWorkflowResponse(
      QueryWorkflowResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.QueryWorkflowResponse res =
        new com.uber.cadence.entities.QueryWorkflowResponse();
    res.setQueryResult(payload(t.getQueryResult()));
    res.setQueryRejected(queryRejected(t.getQueryRejected()));
    return res;
  }

  public static com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatByIdResponse(RecordActivityTaskHeartbeatByIDResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatResponse(RecordActivityTaskHeartbeatResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.entities.ResetWorkflowExecutionResponse
      resetWorkflowExecutionResponse(ResetWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ResetWorkflowExecutionResponse res =
        new com.uber.cadence.entities.ResetWorkflowExecutionResponse();
    res.setRunId(t.getRunId());
    return res;
  }

  public static com.uber.cadence.entities.RespondDecisionTaskCompletedResponse
      respondDecisionTaskCompletedResponse(RespondDecisionTaskCompletedResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.RespondDecisionTaskCompletedResponse res =
        new com.uber.cadence.entities.RespondDecisionTaskCompletedResponse();
    res.setDecisionTask(pollForDecisionTaskResponse(t.getDecisionTask()));
    res.setActivitiesToDispatchLocally(
        activityLocalDispatchInfoMap(t.getActivitiesToDispatchLocallyMap()));
    return res;
  }

  public static com.uber.cadence.entities.ListWorkflowExecutionsResponse
      scanWorkflowExecutionsResponse(ScanWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.CountWorkflowExecutionsResponse
      countWorkflowExecutionsResponse(CountWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.CountWorkflowExecutionsResponse res =
        new com.uber.cadence.entities.CountWorkflowExecutionsResponse();
    res.setCount(t.getCount());
    return res;
  }

  public static com.uber.cadence.entities.DescribeDomainResponse describeDomainResponse(
      DescribeDomainResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.DescribeDomainResponse response =
        new com.uber.cadence.entities.DescribeDomainResponse();
    com.uber.cadence.entities.DomainInfo domainInfo = new com.uber.cadence.entities.DomainInfo();
    response.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    com.uber.cadence.entities.DomainConfiguration domainConfiguration =
        new com.uber.cadence.entities.DomainConfiguration();
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

    com.uber.cadence.entities.DomainReplicationConfiguration replicationConfiguration =
        new com.uber.cadence.entities.DomainReplicationConfiguration();
    response.setReplicationConfiguration(replicationConfiguration);

    replicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    replicationConfiguration.setClusters(
        clusterReplicationConfigurationArrayFromProto(t.getDomain().getClustersList()));

    response.setFailoverVersion(t.getDomain().getFailoverVersion());
    response.setGlobalDomain(t.getDomain().getIsGlobalDomain());
    return response;
  }

  public static com.uber.cadence.entities.ListDomainsResponse listDomainsResponse(
      ListDomainsResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.ListDomainsResponse res =
        new com.uber.cadence.entities.ListDomainsResponse();
    res.setDomains(describeDomainResponseArray(t.getDomainsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static com.uber.cadence.entities.StartWorkflowExecutionResponse
      signalWithStartWorkflowExecutionResponse(SignalWithStartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new com.uber.cadence.entities.StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static com.uber.cadence.entities.SignalWithStartWorkflowExecutionAsyncResponse
      signalWithStartWorkflowExecutionAsyncResponse(
          SignalWithStartWorkflowExecutionAsyncResponse t) {
    return t == null
        ? null
        : new com.uber.cadence.entities.SignalWithStartWorkflowExecutionAsyncResponse();
  }

  public static com.uber.cadence.entities.UpdateDomainResponse updateDomainResponse(
      UpdateDomainResponse t) {
    if (t == null) {
      return null;
    }
    com.uber.cadence.entities.UpdateDomainResponse updateDomainResponse =
        new com.uber.cadence.entities.UpdateDomainResponse();
    com.uber.cadence.entities.DomainInfo domainInfo = new com.uber.cadence.entities.DomainInfo();
    updateDomainResponse.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    com.uber.cadence.entities.DomainConfiguration domainConfiguration =
        new com.uber.cadence.entities.DomainConfiguration();
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

    com.uber.cadence.entities.DomainReplicationConfiguration domainReplicationConfiguration =
        new com.uber.cadence.entities.DomainReplicationConfiguration();
    updateDomainResponse.setReplicationConfiguration(domainReplicationConfiguration);

    domainReplicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    domainReplicationConfiguration.setClusters(
        clusterReplicationConfigurationArrayFromProto(t.getDomain().getClustersList()));
    updateDomainResponse.setFailoverVersion(t.getDomain().getFailoverVersion());
    updateDomainResponse.setGlobalDomain(t.getDomain().getIsGlobalDomain());
    return updateDomainResponse;
  }

  public static com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse
      recordActivityTaskHeartbeatResponse(
          RecordActivityTaskHeartbeatByIDResponse recordActivityTaskHeartbeatByID) {
    if (recordActivityTaskHeartbeatByID == null) {
      return null;
    }
    com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse res =
        new com.uber.cadence.entities.RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(recordActivityTaskHeartbeatByID.getCancelRequested());
    return res;
  }

  public static com.uber.cadence.entities.ResetStickyTaskListResponse resetStickyTaskListResponse(
      ResetStickyTaskListResponse resetStickyTaskList) {
    if (resetStickyTaskList == null) {
      return null;
    }
    com.uber.cadence.entities.ResetStickyTaskListResponse res =
        new com.uber.cadence.entities.ResetStickyTaskListResponse();
    return res;
  }

  public static com.uber.cadence.entities.ClusterInfo clusterInfoResponse(
      GetClusterInfoResponse clusterInfo) {
    if (clusterInfo == null) {
      return null;
    }
    com.uber.cadence.entities.ClusterInfo res = new com.uber.cadence.entities.ClusterInfo();
    res.setSupportedClientVersions(
        TypeMapper.supportedClientVersions(clusterInfo.getSupportedClientVersions()));
    return res;
  }

  public static com.uber.cadence.entities.GetTaskListsByDomainResponse getTaskListsByDomainResponse(
      GetTaskListsByDomainResponse taskListsByDomain) {
    if (taskListsByDomain == null) {
      return null;
    }
    com.uber.cadence.entities.GetTaskListsByDomainResponse res =
        new com.uber.cadence.entities.GetTaskListsByDomainResponse();

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
