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
import static com.uber.cadence.internal.compatibility.thrift.Helpers.byteStringToArray;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToDays;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.durationToSeconds;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.timeToUnixNano;
import static com.uber.cadence.internal.compatibility.thrift.Helpers.toInt64Value;
import static com.uber.cadence.internal.compatibility.thrift.HistoryMapper.history;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.activityLocalDispatchInfoMap;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.activityType;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.autoConfigHint;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.badBinaries;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.clusterReplicationConfigurationArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.dataBlobArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.describeDomainResponseArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.indexedValueTypeMap;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.pendingActivityInfoArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.pendingChildExecutionInfoArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.pendingDecisionInfo;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.pollerInfoArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.queryRejected;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.supportedClientVersions;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.taskListPartitionMetadataArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.taskListStatus;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecutionConfiguration;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecutionInfo;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowExecutionInfoArray;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowQuery;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowQueryMap;
import static com.uber.cadence.internal.compatibility.thrift.TypeMapper.workflowType;

import com.uber.cadence.ClusterInfo;
import com.uber.cadence.CountWorkflowExecutionsResponse;
import com.uber.cadence.DescribeDomainResponse;
import com.uber.cadence.DescribeTaskListResponse;
import com.uber.cadence.DescribeWorkflowExecutionResponse;
import com.uber.cadence.DomainConfiguration;
import com.uber.cadence.DomainInfo;
import com.uber.cadence.DomainReplicationConfiguration;
import com.uber.cadence.GetSearchAttributesResponse;
import com.uber.cadence.GetWorkflowExecutionHistoryResponse;
import com.uber.cadence.ListArchivedWorkflowExecutionsResponse;
import com.uber.cadence.ListClosedWorkflowExecutionsResponse;
import com.uber.cadence.ListDomainsResponse;
import com.uber.cadence.ListOpenWorkflowExecutionsResponse;
import com.uber.cadence.ListTaskListPartitionsResponse;
import com.uber.cadence.ListWorkflowExecutionsResponse;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.PollForDecisionTaskResponse;
import com.uber.cadence.QueryWorkflowResponse;
import com.uber.cadence.RecordActivityTaskHeartbeatResponse;
import com.uber.cadence.ResetWorkflowExecutionResponse;
import com.uber.cadence.RespondDecisionTaskCompletedResponse;
import com.uber.cadence.SignalWithStartWorkflowExecutionAsyncResponse;
import com.uber.cadence.StartWorkflowExecutionAsyncResponse;
import com.uber.cadence.StartWorkflowExecutionResponse;
import com.uber.cadence.UpdateDomainResponse;
import com.uber.cadence.api.v1.WorkflowQuery;

public class ResponseMapper {

  public static StartWorkflowExecutionResponse startWorkflowExecutionResponse(
      com.uber.cadence.api.v1.StartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static StartWorkflowExecutionAsyncResponse startWorkflowExecutionAsyncResponse(
      com.uber.cadence.api.v1.StartWorkflowExecutionAsyncResponse t) {
    return t == null ? null : new StartWorkflowExecutionAsyncResponse();
  }

  public static DescribeTaskListResponse describeTaskListResponse(
      com.uber.cadence.api.v1.DescribeTaskListResponse t) {
    if (t == null) {
      return null;
    }
    DescribeTaskListResponse describeTaskListResponse = new DescribeTaskListResponse();
    describeTaskListResponse.setPollers(pollerInfoArray(t.getPollersList()));
    describeTaskListResponse.setTaskListStatus(taskListStatus(t.getTaskListStatus()));
    return describeTaskListResponse;
  }

  public static DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse(
      com.uber.cadence.api.v1.DescribeWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        new DescribeWorkflowExecutionResponse();
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

  public static ClusterInfo getClusterInfoResponse(
      com.uber.cadence.api.v1.GetClusterInfoResponse t) {
    if (t == null) {
      return null;
    }
    ClusterInfo clusterInfo = new ClusterInfo();
    clusterInfo.setSupportedClientVersions(supportedClientVersions(t.getSupportedClientVersions()));
    return clusterInfo;
  }

  public static GetSearchAttributesResponse getSearchAttributesResponse(
      com.uber.cadence.api.v1.GetSearchAttributesResponse t) {
    if (t == null) {
      return null;
    }
    GetSearchAttributesResponse getSearchAttributesResponse = new GetSearchAttributesResponse();
    getSearchAttributesResponse.setKeys(indexedValueTypeMap(t.getKeysMap()));
    return getSearchAttributesResponse;
  }

  public static GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistoryResponse(
      com.uber.cadence.api.v1.GetWorkflowExecutionHistoryResponse t) {
    if (t == null) {
      return null;
    }
    GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistoryResponse =
        new GetWorkflowExecutionHistoryResponse();
    getWorkflowExecutionHistoryResponse.setHistory(history(t.getHistory()));
    getWorkflowExecutionHistoryResponse.setRawHistory(dataBlobArray(t.getRawHistoryList()));
    getWorkflowExecutionHistoryResponse.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    getWorkflowExecutionHistoryResponse.setArchived(t.getArchived());
    return getWorkflowExecutionHistoryResponse;
  }

  public static ListArchivedWorkflowExecutionsResponse listArchivedWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.ListArchivedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    ListArchivedWorkflowExecutionsResponse res = new ListArchivedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static ListClosedWorkflowExecutionsResponse listClosedWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.ListClosedWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    ListClosedWorkflowExecutionsResponse res = new ListClosedWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static ListOpenWorkflowExecutionsResponse listOpenWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.ListOpenWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    ListOpenWorkflowExecutionsResponse res = new ListOpenWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static ListTaskListPartitionsResponse listTaskListPartitionsResponse(
      com.uber.cadence.api.v1.ListTaskListPartitionsResponse t) {
    if (t == null) {
      return null;
    }
    ListTaskListPartitionsResponse res = new ListTaskListPartitionsResponse();
    res.setActivityTaskListPartitions(
        taskListPartitionMetadataArray(t.getActivityTaskListPartitionsList()));
    res.setDecisionTaskListPartitions(
        taskListPartitionMetadataArray(t.getDecisionTaskListPartitionsList()));
    return res;
  }

  public static ListWorkflowExecutionsResponse listWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.ListWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    ListWorkflowExecutionsResponse res = new ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static PollForActivityTaskResponse pollForActivityTaskResponse(
      com.uber.cadence.api.v1.PollForActivityTaskResponse t) {
    if (t == null) {
      return null;
    }
    PollForActivityTaskResponse res = new PollForActivityTaskResponse();
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
    res.setAutoConfigHint(autoConfigHint(t.getAutoConfigHint()));
    return res;
  }

  public static PollForDecisionTaskResponse pollForDecisionTaskResponse(
      com.uber.cadence.api.v1.PollForDecisionTaskResponse t) {
    if (t == null) {
      return null;
    }
    PollForDecisionTaskResponse res = new PollForDecisionTaskResponse();
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
    res.setAutoConfigHint(autoConfigHint(t.getAutoConfigHint()));
    return res;
  }

  public static QueryWorkflowResponse queryWorkflowResponse(
      com.uber.cadence.api.v1.QueryWorkflowResponse t) {
    if (t == null) {
      return null;
    }
    QueryWorkflowResponse res = new QueryWorkflowResponse();
    res.setQueryResult(payload(t.getQueryResult()));
    res.setQueryRejected(queryRejected(t.getQueryRejected()));
    return res;
  }

  public static RecordActivityTaskHeartbeatResponse recordActivityTaskHeartbeatByIdResponse(
      com.uber.cadence.api.v1.RecordActivityTaskHeartbeatByIDResponse t) {
    if (t == null) {
      return null;
    }
    RecordActivityTaskHeartbeatResponse res = new RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static RecordActivityTaskHeartbeatResponse recordActivityTaskHeartbeatResponse(
      com.uber.cadence.api.v1.RecordActivityTaskHeartbeatResponse t) {
    if (t == null) {
      return null;
    }
    RecordActivityTaskHeartbeatResponse res = new RecordActivityTaskHeartbeatResponse();
    res.setCancelRequested(t.getCancelRequested());
    return res;
  }

  public static ResetWorkflowExecutionResponse resetWorkflowExecutionResponse(
      com.uber.cadence.api.v1.ResetWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    ResetWorkflowExecutionResponse res = new ResetWorkflowExecutionResponse();
    res.setRunId(t.getRunId());
    return res;
  }

  public static RespondDecisionTaskCompletedResponse respondDecisionTaskCompletedResponse(
      com.uber.cadence.api.v1.RespondDecisionTaskCompletedResponse t) {
    if (t == null) {
      return null;
    }
    RespondDecisionTaskCompletedResponse res = new RespondDecisionTaskCompletedResponse();
    res.setDecisionTask(pollForDecisionTaskResponse(t.getDecisionTask()));
    res.setActivitiesToDispatchLocally(
        activityLocalDispatchInfoMap(t.getActivitiesToDispatchLocallyMap()));
    return res;
  }

  public static ListWorkflowExecutionsResponse scanWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.ScanWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    ListWorkflowExecutionsResponse res = new ListWorkflowExecutionsResponse();
    res.setExecutions(workflowExecutionInfoArray(t.getExecutionsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static CountWorkflowExecutionsResponse countWorkflowExecutionsResponse(
      com.uber.cadence.api.v1.CountWorkflowExecutionsResponse t) {
    if (t == null) {
      return null;
    }
    CountWorkflowExecutionsResponse res = new CountWorkflowExecutionsResponse();
    res.setCount(t.getCount());
    return res;
  }

  public static DescribeDomainResponse describeDomainResponse(
      com.uber.cadence.api.v1.DescribeDomainResponse t) {
    if (t == null) {
      return null;
    }
    DescribeDomainResponse response = new DescribeDomainResponse();
    DomainInfo domainInfo = new DomainInfo();
    response.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    DomainConfiguration domainConfiguration = new DomainConfiguration();
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

    DomainReplicationConfiguration replicationConfiguration = new DomainReplicationConfiguration();
    response.setReplicationConfiguration(replicationConfiguration);

    replicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    replicationConfiguration.setClusters(
        clusterReplicationConfigurationArray(t.getDomain().getClustersList()));

    response.setFailoverVersion(t.getDomain().getFailoverVersion());
    response.setIsGlobalDomain(t.getDomain().getIsGlobalDomain());
    return response;
  }

  public static ListDomainsResponse listDomainsResponse(
      com.uber.cadence.api.v1.ListDomainsResponse t) {
    if (t == null) {
      return null;
    }
    ListDomainsResponse res = new ListDomainsResponse();
    res.setDomains(describeDomainResponseArray(t.getDomainsList()));
    res.setNextPageToken(byteStringToArray(t.getNextPageToken()));
    return res;
  }

  public static StartWorkflowExecutionResponse signalWithStartWorkflowExecutionResponse(
      com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionResponse t) {
    if (t == null) {
      return null;
    }
    StartWorkflowExecutionResponse startWorkflowExecutionResponse =
        new StartWorkflowExecutionResponse();
    startWorkflowExecutionResponse.setRunId(t.getRunId());
    return startWorkflowExecutionResponse;
  }

  public static SignalWithStartWorkflowExecutionAsyncResponse
      signalWithStartWorkflowExecutionAsyncResponse(
          com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncResponse t) {
    return t == null ? null : new SignalWithStartWorkflowExecutionAsyncResponse();
  }

  public static UpdateDomainResponse updateDomainResponse(
      com.uber.cadence.api.v1.UpdateDomainResponse t) {
    if (t == null) {
      return null;
    }
    UpdateDomainResponse updateDomainResponse = new UpdateDomainResponse();
    DomainInfo domainInfo = new DomainInfo();
    updateDomainResponse.setDomainInfo(domainInfo);

    domainInfo.setName(t.getDomain().getName());
    domainInfo.setStatus(domainStatus(t.getDomain().getStatus()));
    domainInfo.setDescription(t.getDomain().getDescription());
    domainInfo.setOwnerEmail(t.getDomain().getOwnerEmail());
    domainInfo.setData(t.getDomain().getDataMap());
    domainInfo.setUuid(t.getDomain().getId());

    DomainConfiguration domainConfiguration = new DomainConfiguration();
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

    DomainReplicationConfiguration domainReplicationConfiguration =
        new DomainReplicationConfiguration();
    updateDomainResponse.setReplicationConfiguration(domainReplicationConfiguration);

    domainReplicationConfiguration.setActiveClusterName(t.getDomain().getActiveClusterName());
    domainReplicationConfiguration.setClusters(
        clusterReplicationConfigurationArray(t.getDomain().getClustersList()));
    updateDomainResponse.setFailoverVersion(t.getDomain().getFailoverVersion());
    updateDomainResponse.setIsGlobalDomain(t.getDomain().getIsGlobalDomain());
    return updateDomainResponse;
  }
}
