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

import static com.uber.cadence.internal.compatibility.proto.mappers.DecisionMapper.decisionArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.archivalStatus;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.decisionTaskFailedCause;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.eventFilterType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.queryConsistencyLevel;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.queryRejectCondition;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.queryTaskCompletedType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.taskListType;
import static com.uber.cadence.internal.compatibility.proto.mappers.EnumMapper.workflowIdReusePolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.arrayToByteString;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.daysToDuration;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.newFieldMask;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.nullToEmpty;
import static com.uber.cadence.internal.compatibility.proto.mappers.Helpers.secondsToDuration;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.badBinaries;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.clusterReplicationConfigurationArray;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.failure;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.header;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.memo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.payload;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.retryPolicy;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.searchAttributes;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.startTimeFilter;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.statusFilter;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.stickyExecutionAttributes;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskList;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.taskListMetadata;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workerVersionInfo;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecution;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowExecutionFilter;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowQuery;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowQueryResultMap;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowType;
import static com.uber.cadence.internal.compatibility.proto.mappers.TypeMapper.workflowTypeFilter;

import com.uber.cadence.api.v1.CountWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.DeprecateDomainRequest;
import com.uber.cadence.api.v1.DescribeDomainRequest;
import com.uber.cadence.api.v1.DescribeTaskListRequest;
import com.uber.cadence.api.v1.DescribeWorkflowExecutionRequest;
import com.uber.cadence.api.v1.GetTaskListsByDomainRequest;
import com.uber.cadence.api.v1.GetWorkflowExecutionHistoryRequest;
import com.uber.cadence.api.v1.ListArchivedWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListClosedWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListDomainsRequest;
import com.uber.cadence.api.v1.ListOpenWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.ListTaskListPartitionsRequest;
import com.uber.cadence.api.v1.ListWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.PollForActivityTaskRequest;
import com.uber.cadence.api.v1.PollForDecisionTaskRequest;
import com.uber.cadence.api.v1.QueryWorkflowRequest;
import com.uber.cadence.api.v1.RecordActivityTaskHeartbeatByIDRequest;
import com.uber.cadence.api.v1.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.api.v1.RefreshWorkflowTasksRequest;
import com.uber.cadence.api.v1.RegisterDomainRequest;
import com.uber.cadence.api.v1.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.api.v1.ResetStickyTaskListRequest;
import com.uber.cadence.api.v1.ResetWorkflowExecutionRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCanceledByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCanceledRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCompletedByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskCompletedRequest;
import com.uber.cadence.api.v1.RespondActivityTaskFailedByIDRequest;
import com.uber.cadence.api.v1.RespondActivityTaskFailedRequest;
import com.uber.cadence.api.v1.RespondDecisionTaskCompletedRequest;
import com.uber.cadence.api.v1.RespondDecisionTaskFailedRequest;
import com.uber.cadence.api.v1.RespondQueryTaskCompletedRequest;
import com.uber.cadence.api.v1.RestartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.ScanWorkflowExecutionsRequest;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.SignalWorkflowExecutionRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionAsyncRequest;
import com.uber.cadence.api.v1.StartWorkflowExecutionRequest;
import com.uber.cadence.api.v1.TerminateWorkflowExecutionRequest;
import com.uber.cadence.api.v1.UpdateDomainRequest;
import com.uber.cadence.api.v1.UpdateDomainRequest.Builder;
import com.uber.cadence.api.v1.WorkflowQueryResult;
import java.util.ArrayList;
import java.util.List;

public class RequestMapper {

  private static final String DomainUpdateDescriptionField = "description";
  private static final String DomainUpdateOwnerEmailField = "owner_email";
  private static final String DomainUpdateDataField = "data";
  private static final String DomainUpdateRetentionPeriodField =
      "workflow_execution_retention_period";

  private static final String DomainUpdateBadBinariesField = "bad_binaries";
  private static final String DomainUpdateHistoryArchivalStatusField = "history_archival_status";
  private static final String DomainUpdateHistoryArchivalURIField = "history_archival_uri";
  private static final String DomainUpdateVisibilityArchivalStatusField =
      "visibility_archival_status";
  private static final String DomainUpdateVisibilityArchivalURIField = "visibility_archival_uri";
  private static final String DomainUpdateActiveClusterNameField = "active_cluster_name";
  private static final String DomainUpdateClustersField = "clusters";
  private static final String DomainUpdateDeleteBadBinaryField = "delete_bad_binary";
  private static final String DomainUpdateFailoverTimeoutField = "failover_timeout";

  public static CountWorkflowExecutionsRequest countWorkflowExecutionsRequest(
      com.uber.cadence.entities.CountWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    CountWorkflowExecutionsRequest.Builder request =
        CountWorkflowExecutionsRequest.newBuilder().setDomain(t.getDomain());
    if (t.getQuery() != null) {
      request.setQuery(t.getQuery());
    }
    return request.build();
  }

  public static DescribeTaskListRequest describeTaskListRequest(
      com.uber.cadence.entities.DescribeTaskListRequest t) {
    if (t == null) {
      return null;
    }
    return DescribeTaskListRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .setTaskListType(taskListType(t.getTaskListType()))
        .setIncludeTaskListStatus(t.isIncludeTaskListStatus())
        .build();
  }

  public static ListArchivedWorkflowExecutionsRequest listArchivedWorkflowExecutionsRequest(
      com.uber.cadence.entities.ListArchivedWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    ListArchivedWorkflowExecutionsRequest.Builder request =
        ListArchivedWorkflowExecutionsRequest.newBuilder()
            .setDomain(t.getDomain())
            .setPageSize(t.getPageSize());
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    if (t.getQuery() != null) {
      request.setQuery(t.getQuery());
    }
    return request.build();
  }

  public static RequestCancelWorkflowExecutionRequest requestCancelWorkflowExecutionRequest(
      com.uber.cadence.entities.RequestCancelWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    RequestCancelWorkflowExecutionRequest.Builder builder =
        RequestCancelWorkflowExecutionRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
            .setRequestId(t.getRequestId());
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    if (t.getFirstExecutionRunID() != null) {
      builder.setFirstExecutionRunId(t.getFirstExecutionRunID());
    }
    if (t.getCause() != null) {
      builder.setCause(t.getCause());
    }
    return builder.build();
  }

  public static ResetStickyTaskListRequest resetStickyTaskListRequest(
      com.uber.cadence.entities.ResetStickyTaskListRequest t) {
    if (t == null) {
      return null;
    }
    return ResetStickyTaskListRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .build();
  }

  public static ResetWorkflowExecutionRequest resetWorkflowExecutionRequest(
      com.uber.cadence.entities.ResetWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return ResetWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setReason(t.getReason())
        .setDecisionFinishEventId(t.getDecisionFinishEventId())
        .setRequestId(t.getRequestId())
        .setSkipSignalReapply(t.isSkipSignalReapply())
        .build();
  }

  public static RespondActivityTaskCanceledByIDRequest respondActivityTaskCanceledByIdRequest(
      com.uber.cadence.entities.RespondActivityTaskCanceledByIDRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskCanceledByIDRequest.Builder builder =
        RespondActivityTaskCanceledByIDRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
            .setActivityId(t.getActivityID())
            .setDetails(payload(t.getDetails()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondActivityTaskCanceledRequest respondActivityTaskCanceledRequest(
      com.uber.cadence.entities.RespondActivityTaskCanceledRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskCanceledRequest.Builder builder =
        RespondActivityTaskCanceledRequest.newBuilder().setDetails(payload(t.getDetails()));
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondActivityTaskCompletedByIDRequest respondActivityTaskCompletedByIdRequest(
      com.uber.cadence.entities.RespondActivityTaskCompletedByIDRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskCompletedByIDRequest.Builder builder =
        RespondActivityTaskCompletedByIDRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
            .setActivityId(t.getActivityID())
            .setResult(payload(t.getResult()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondActivityTaskCompletedRequest respondActivityTaskCompletedRequest(
      com.uber.cadence.entities.RespondActivityTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskCompletedRequest.Builder builder =
        RespondActivityTaskCompletedRequest.newBuilder().setResult(payload(t.getResult()));
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondActivityTaskFailedByIDRequest respondActivityTaskFailedByIdRequest(
      com.uber.cadence.entities.RespondActivityTaskFailedByIDRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskFailedByIDRequest.Builder builder =
        RespondActivityTaskFailedByIDRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
            .setActivityId(t.getActivityID())
            .setFailure(failure(t.getReason(), t.getDetails()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondActivityTaskFailedRequest respondActivityTaskFailedRequest(
      com.uber.cadence.entities.RespondActivityTaskFailedRequest t) {
    if (t == null) {
      return null;
    }
    RespondActivityTaskFailedRequest.Builder builder =
        RespondActivityTaskFailedRequest.newBuilder()
            .setFailure(failure(t.getReason(), t.getDetails()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    return builder.build();
  }

  public static RespondDecisionTaskCompletedRequest respondDecisionTaskCompletedRequest(
      com.uber.cadence.entities.RespondDecisionTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    RespondDecisionTaskCompletedRequest.Builder builder =
        RespondDecisionTaskCompletedRequest.newBuilder()
            .addAllDecisions(decisionArray(t.getDecisions()))
            .setStickyAttributes(stickyExecutionAttributes(t.getStickyAttributes()))
            .setReturnNewDecisionTask(t.isReturnNewDecisionTask())
            .setForceCreateNewDecisionTask(t.isForceCreateNewDecisionTask())
            .putAllQueryResults(workflowQueryResultMap(t.getQueryResults()));
    if (t.getExecutionContext() != null) {
      builder.setExecutionContext(arrayToByteString(t.getExecutionContext()));
    }
    if (t.getBinaryChecksum() != null) {
      builder.setBinaryChecksum(t.getBinaryChecksum());
    }
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondDecisionTaskFailedRequest respondDecisionTaskFailedRequest(
      com.uber.cadence.entities.RespondDecisionTaskFailedRequest t) {
    if (t == null) {
      return null;
    }
    RespondDecisionTaskFailedRequest.Builder builder =
        RespondDecisionTaskFailedRequest.newBuilder()
            .setCause(decisionTaskFailedCause(t.getCause()))
            .setDetails(payload(t.getDetails()));
    if (t.getBinaryChecksum() != null) {
      builder.setBinaryChecksum(t.getBinaryChecksum());
    }
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RespondQueryTaskCompletedRequest respondQueryTaskCompletedRequest(
      com.uber.cadence.entities.RespondQueryTaskCompletedRequest t) {
    if (t == null) {
      return null;
    }
    WorkflowQueryResult.Builder wqBuilder =
        WorkflowQueryResult.newBuilder()
            .setResultType(queryTaskCompletedType(t.getCompletedType()))
            .setAnswer(payload(t.getQueryResult()));
    if (t.getErrorMessage() != null) {
      wqBuilder.setErrorMessage(t.getErrorMessage());
    }
    RespondQueryTaskCompletedRequest.Builder builder =
        RespondQueryTaskCompletedRequest.newBuilder()
            .setResult(wqBuilder.build())
            .setWorkerVersionInfo(workerVersionInfo(t.getWorkerVersionInfo()));
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    return builder.build();
  }

  public static ScanWorkflowExecutionsRequest scanWorkflowExecutionsRequest(
      com.uber.cadence.entities.ListWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    ScanWorkflowExecutionsRequest.Builder request =
        ScanWorkflowExecutionsRequest.newBuilder()
            .setDomain(t.getDomain())
            .setPageSize(t.getPageSize());
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    if (t.getQuery() != null) {
      request.setQuery(t.getQuery());
    }
    return request.build();
  }

  public static DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest(
      com.uber.cadence.entities.DescribeWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return DescribeWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .build();
  }

  public static GetWorkflowExecutionHistoryRequest getWorkflowExecutionHistoryRequest(
      com.uber.cadence.entities.GetWorkflowExecutionHistoryRequest t) {
    if (t == null) {
      return null;
    }
    GetWorkflowExecutionHistoryRequest.Builder builder =
        GetWorkflowExecutionHistoryRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(workflowExecution(t.getExecution()))
            .setPageSize(t.getMaximumPageSize())
            .setWaitForNewEvent(t.isWaitForNewEvent())
            .setHistoryEventFilterType(eventFilterType(t.getHistoryEventFilterType()))
            .setSkipArchival(t.isSkipArchival());
    if (t.getNextPageToken() != null) {
      builder.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    return builder.build();
  }

  public static SignalWithStartWorkflowExecutionRequest signalWithStartWorkflowExecutionRequest(
      com.uber.cadence.entities.SignalWithStartWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    StartWorkflowExecutionRequest.Builder builder =
        StartWorkflowExecutionRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowId(t.getWorkflowId())
            .setWorkflowType(workflowType(t.getWorkflowType()))
            .setTaskList(taskList(t.getTaskList()))
            .setInput(payload(t.getInput()))
            .setExecutionStartToCloseTimeout(
                secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
            .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
            .setRequestId(t.getRequestId())
            .setMemo(memo(t.getMemo()))
            .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
            .setHeader(header(t.getHeader()));
    if (t.getRetryPolicy() != null) {
      builder.setRetryPolicy(retryPolicy(t.getRetryPolicy()));
    }
    builder.setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()));
    if (t.getWorkflowIdReusePolicy() != null) {
      builder.setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()));
    }
    if (t.getCronSchedule() != null) {
      builder.setCronSchedule(t.getCronSchedule());
    }
    if (t.getDelayStartSeconds() > 0) {
      builder.setDelayStart(secondsToDuration(t.getDelayStartSeconds()));
    }
    builder.setJitterStart(secondsToDuration(t.getJitterStartSeconds()));

    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    SignalWithStartWorkflowExecutionRequest.Builder sb =
        SignalWithStartWorkflowExecutionRequest.newBuilder()
            .setStartRequest(builder.build())
            .setSignalName(t.getSignalName())
            .setSignalInput(payload(t.getSignalInput()));
    if (t.getControl() != null) {
      sb.setControl(arrayToByteString(t.getControl()));
    }
    return sb.build();
  }

  public static SignalWithStartWorkflowExecutionAsyncRequest
      signalWithStartWorkflowExecutionAsyncRequest(
          com.uber.cadence.entities.SignalWithStartWorkflowExecutionAsyncRequest t) {
    if (t == null) {
      return null;
    }
    SignalWithStartWorkflowExecutionAsyncRequest.Builder builder =
        SignalWithStartWorkflowExecutionAsyncRequest.newBuilder();
    if (t.getRequest() != null) {
      builder.setRequest(signalWithStartWorkflowExecutionRequest(t.getRequest()));
    }
    return builder.build();
  }

  public static SignalWorkflowExecutionRequest signalWorkflowExecutionRequest(
      com.uber.cadence.entities.SignalWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    SignalWorkflowExecutionRequest.Builder builder =
        SignalWorkflowExecutionRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
            .setSignalName(t.getSignalName())
            .setSignalInput(payload(t.getInput()))
            .setRequestId(t.getRequestId());
    if (t.getControl() != null) {
      builder.setControl(arrayToByteString(t.getControl()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static StartWorkflowExecutionRequest startWorkflowExecutionRequest(
      com.uber.cadence.entities.StartWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    StartWorkflowExecutionRequest.Builder request =
        StartWorkflowExecutionRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowId(t.getWorkflowId())
            .setWorkflowType(workflowType(t.getWorkflowType()))
            .setTaskList(taskList(t.getTaskList()))
            .setInput(payload(t.getInput()))
            .setRequestId(t.getRequestId())
            .setExecutionStartToCloseTimeout(
                secondsToDuration(t.getExecutionStartToCloseTimeoutSeconds()))
            .setTaskStartToCloseTimeout(secondsToDuration(t.getTaskStartToCloseTimeoutSeconds()))
            .setWorkflowIdReusePolicy(workflowIdReusePolicy(t.getWorkflowIdReusePolicy()))
            .setMemo(memo(t.getMemo()))
            .setSearchAttributes(searchAttributes(t.getSearchAttributes()))
            .setHeader(header(t.getHeader()))
            .setDelayStart(secondsToDuration(t.getDelayStartSeconds()))
            .setJitterStart(secondsToDuration(t.getJitterStartSeconds()));
    if (t.getRetryPolicy() != null) {
      request.setRetryPolicy(retryPolicy(t.getRetryPolicy()));
    }
    if (t.getCronSchedule() != null) {
      request.setCronSchedule(t.getCronSchedule());
    }
    if (t.getIdentity() != null) {
      request.setIdentity(t.getIdentity());
    }
    return request.build();
  }

  public static StartWorkflowExecutionAsyncRequest startWorkflowExecutionAsyncRequest(
      com.uber.cadence.entities.StartWorkflowExecutionAsyncRequest t) {
    if (t == null) {
      return null;
    }
    StartWorkflowExecutionAsyncRequest.Builder builder =
        StartWorkflowExecutionAsyncRequest.newBuilder();
    if (t.getRequest() != null) {
      builder.setRequest(startWorkflowExecutionRequest(t.getRequest()));
    }
    return builder.build();
  }

  public static TerminateWorkflowExecutionRequest terminateWorkflowExecutionRequest(
      com.uber.cadence.entities.TerminateWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    TerminateWorkflowExecutionRequest.Builder builder =
        TerminateWorkflowExecutionRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
            .setReason(t.getReason())
            .setDetails(payload(t.getDetails()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    if (t.getFirstExecutionRunID() != null) {
      builder.setFirstExecutionRunId(t.getFirstExecutionRunID());
    }
    return builder.build();
  }

  public static DeprecateDomainRequest deprecateDomainRequest(
      com.uber.cadence.entities.DeprecateDomainRequest t) {
    if (t == null) {
      return null;
    }
    return DeprecateDomainRequest.newBuilder()
        .setName(t.getName())
        .setSecurityToken(t.getSecurityToken())
        .build();
  }

  public static DescribeDomainRequest describeDomainRequest(
      com.uber.cadence.entities.DescribeDomainRequest t) {
    if (t == null) {
      return null;
    }
    if (t.getUuid() != null) {
      return DescribeDomainRequest.newBuilder().setId(t.getUuid()).build();
    }
    if (t.getName() != null) {
      return DescribeDomainRequest.newBuilder().setName(t.getName()).build();
    }
    throw new IllegalArgumentException("neither one of field is set for DescribeDomainRequest");
  }

  public static ListDomainsRequest listDomainsRequest(
      com.uber.cadence.entities.ListDomainsRequest t) {
    if (t == null) {
      return null;
    }
    ListDomainsRequest.Builder request =
        ListDomainsRequest.newBuilder().setPageSize(t.getPageSize());
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    return request.build();
  }

  public static ListTaskListPartitionsRequest listTaskListPartitionsRequest(
      com.uber.cadence.entities.ListTaskListPartitionsRequest t) {
    if (t == null) {
      return null;
    }
    return ListTaskListPartitionsRequest.newBuilder()
        .setDomain(t.getDomain())
        .setTaskList(taskList(t.getTaskList()))
        .build();
  }

  public static ListWorkflowExecutionsRequest listWorkflowExecutionsRequest(
      com.uber.cadence.entities.ListWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    ListWorkflowExecutionsRequest.Builder request =
        ListWorkflowExecutionsRequest.newBuilder()
            .setDomain(t.getDomain())
            .setPageSize(t.getPageSize());
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    if (t.getQuery() != null) {
      request.setQuery(t.getQuery());
    }
    return request.build();
  }

  public static PollForActivityTaskRequest pollForActivityTaskRequest(
      com.uber.cadence.entities.PollForActivityTaskRequest t) {
    if (t == null) {
      return null;
    }
    PollForActivityTaskRequest.Builder builder =
        PollForActivityTaskRequest.newBuilder()
            .setDomain(t.getDomain())
            .setTaskList(taskList(t.getTaskList()))
            .setTaskListMetadata(taskListMetadata(t.getTaskListMetadata()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static PollForDecisionTaskRequest pollForDecisionTaskRequest(
      com.uber.cadence.entities.PollForDecisionTaskRequest t) {
    if (t == null) {
      return null;
    }
    PollForDecisionTaskRequest.Builder builder =
        PollForDecisionTaskRequest.newBuilder()
            .setDomain(t.getDomain())
            .setTaskList(taskList(t.getTaskList()));
    if (t.getBinaryChecksum() != null) {
      builder.setBinaryChecksum(t.getBinaryChecksum());
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static QueryWorkflowRequest queryWorkflowRequest(
      com.uber.cadence.entities.QueryWorkflowRequest t) {
    if (t == null) {
      return null;
    }
    return QueryWorkflowRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getExecution()))
        .setQuery(workflowQuery(t.getQuery()))
        .setQueryRejectCondition(queryRejectCondition(t.getQueryRejectCondition()))
        .setQueryConsistencyLevel(queryConsistencyLevel(t.getQueryConsistencyLevel()))
        .build();
  }

  public static RecordActivityTaskHeartbeatByIDRequest recordActivityTaskHeartbeatByIdRequest(
      com.uber.cadence.entities.RecordActivityTaskHeartbeatByIDRequest t) {
    if (t == null) {
      return null;
    }
    RecordActivityTaskHeartbeatByIDRequest.Builder builder =
        RecordActivityTaskHeartbeatByIDRequest.newBuilder()
            .setDomain(t.getDomain())
            .setWorkflowExecution(TypeMapper.workflowRunPair(t.getWorkflowID(), t.getRunID()))
            .setActivityId(t.getActivityID())
            .setDetails(payload(t.getDetails()));
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RecordActivityTaskHeartbeatRequest recordActivityTaskHeartbeatRequest(
      com.uber.cadence.entities.RecordActivityTaskHeartbeatRequest t) {
    if (t == null) {
      return null;
    }
    RecordActivityTaskHeartbeatRequest.Builder builder =
        RecordActivityTaskHeartbeatRequest.newBuilder().setDetails(payload(t.getDetails()));
    if (t.getTaskToken() != null) {
      builder.setTaskToken(arrayToByteString(t.getTaskToken()));
    }
    if (t.getIdentity() != null) {
      builder.setIdentity(t.getIdentity());
    }
    return builder.build();
  }

  public static RegisterDomainRequest registerDomainRequest(
      com.uber.cadence.entities.RegisterDomainRequest t) {
    if (t == null) {
      return null;
    }
    RegisterDomainRequest request =
        RegisterDomainRequest.newBuilder()
            .setName(t.getName())
            .setDescription(Helpers.nullToEmpty(t.getDescription()))
            .setOwnerEmail(Helpers.nullToEmpty(t.getOwnerEmail()))
            .setWorkflowExecutionRetentionPeriod(
                daysToDuration(t.getWorkflowExecutionRetentionPeriodInDays()))
            .addAllClusters(clusterReplicationConfigurationArray(t.getClusters()))
            .setActiveClusterName(Helpers.nullToEmpty(t.getActiveClusterName()))
            .putAllData(Helpers.nullToEmpty(t.getData()))
            .setSecurityToken(Helpers.nullToEmpty(t.getSecurityToken()))
            .setIsGlobalDomain(nullToEmpty(t.isGlobalDomain()))
            .setHistoryArchivalStatus(archivalStatus(t.getHistoryArchivalStatus()))
            .setHistoryArchivalUri(Helpers.nullToEmpty(t.getHistoryArchivalURI()))
            .setVisibilityArchivalStatus(archivalStatus(t.getVisibilityArchivalStatus()))
            .setVisibilityArchivalUri(Helpers.nullToEmpty(t.getVisibilityArchivalURI()))
            .build();
    return request;
  }

  public static RestartWorkflowExecutionRequest restartWorkflowExecutionRequest(
      com.uber.cadence.entities.RestartWorkflowExecutionRequest t) {
    if (t == null) {
      return null;
    }
    return RestartWorkflowExecutionRequest.newBuilder()
        .setDomain(t.getDomain())
        .setWorkflowExecution(workflowExecution(t.getWorkflowExecution()))
        .setReason(t.getReason())
        .setIdentity(t.getIdentity())
        .build();
  }

  public static UpdateDomainRequest updateDomainRequest(
      com.uber.cadence.entities.UpdateDomainRequest t) {
    if (t == null) {
      return null;
    }
    Builder request =
        UpdateDomainRequest.newBuilder()
            .setName(t.getName())
            .setSecurityToken(t.getSecurityToken());

    List<String> fields = new ArrayList<>();
    com.uber.cadence.entities.UpdateDomainInfo updatedInfo = t.getUpdatedInfo();
    if (updatedInfo != null) {
      if (updatedInfo.getDescription() != null) {
        request.setDescription(updatedInfo.getDescription());
        fields.add(DomainUpdateDescriptionField);
      }
      if (updatedInfo.getOwnerEmail() != null) {
        request.setOwnerEmail(updatedInfo.getOwnerEmail());
        fields.add(DomainUpdateOwnerEmailField);
      }
      if (updatedInfo.getData() != null) {
        updatedInfo.setData(updatedInfo.getData());
        fields.add(DomainUpdateDataField);
      }
    }
    com.uber.cadence.entities.DomainConfiguration configuration = t.getConfiguration();
    if (configuration != null) {
      if (configuration.getWorkflowExecutionRetentionPeriodInDays() > 0) {
        request.setWorkflowExecutionRetentionPeriod(
            daysToDuration(configuration.getWorkflowExecutionRetentionPeriodInDays()));
        fields.add(DomainUpdateRetentionPeriodField);
      }
      // if t.EmitMetric != null {} - DEPRECATED
      if (configuration.getBadBinaries() != null) {
        request.setBadBinaries(badBinaries(configuration.getBadBinaries()));
        fields.add(DomainUpdateBadBinariesField);
      }
      if (configuration.getHistoryArchivalStatus() != null) {
        request.setHistoryArchivalStatus(archivalStatus(configuration.getHistoryArchivalStatus()));
        fields.add(DomainUpdateHistoryArchivalStatusField);
      }
      if (configuration.getHistoryArchivalURI() != null) {
        request.setHistoryArchivalUri(configuration.getHistoryArchivalURI());
        fields.add(DomainUpdateHistoryArchivalURIField);
      }
      if (configuration.getVisibilityArchivalStatus() != null) {
        request.setVisibilityArchivalStatus(
            archivalStatus(configuration.getVisibilityArchivalStatus()));
        fields.add(DomainUpdateVisibilityArchivalStatusField);
      }
      if (configuration.getVisibilityArchivalURI() != null) {
        request.setVisibilityArchivalUri(configuration.getVisibilityArchivalURI());
        fields.add(DomainUpdateVisibilityArchivalURIField);
      }
    }
    com.uber.cadence.entities.DomainReplicationConfiguration replicationConfiguration =
        t.getReplicationConfiguration();
    if (replicationConfiguration != null) {
      if (replicationConfiguration.getActiveClusterName() != null) {
        request.setActiveClusterName(replicationConfiguration.getActiveClusterName());
        fields.add(DomainUpdateActiveClusterNameField);
      }
      if (replicationConfiguration.getClusters() != null) {
        request.addAllClusters(
            clusterReplicationConfigurationArray(replicationConfiguration.getClusters()));
        fields.add(DomainUpdateClustersField);
      }
    }
    if (t.getDeleteBadBinary() != null) {
      request.setDeleteBadBinary(t.getDeleteBadBinary());
      fields.add(DomainUpdateDeleteBadBinaryField);
    }
    if (t.getFailoverTimeoutInSeconds() > 0) {
      request.setFailoverTimeout(secondsToDuration(t.getFailoverTimeoutInSeconds()));
      fields.add(DomainUpdateFailoverTimeoutField);
    }

    request.setUpdateMask(newFieldMask(fields));

    return request.build();
  }

  public static ListClosedWorkflowExecutionsRequest listClosedWorkflowExecutionsRequest(
      com.uber.cadence.entities.ListClosedWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    ListClosedWorkflowExecutionsRequest.Builder request =
        ListClosedWorkflowExecutionsRequest.newBuilder()
            .setDomain(t.getDomain())
            .setPageSize(t.getMaximumPageSize());
    if (t.getExecutionFilter() != null) {
      request.setExecutionFilter(workflowExecutionFilter(t.getExecutionFilter()));
    }
    if (t.getTypeFilter() != null) {
      request.setTypeFilter(workflowTypeFilter(t.getTypeFilter()));
    }
    if (t.getStatusFilter() != null) {
      request.setStatusFilter(statusFilter(t.getStatusFilter()));
    }
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    if (t.getStartTimeFilter() != null) {
      request.setStartTimeFilter(startTimeFilter(t.getStartTimeFilter()));
    }
    return request.build();
  }

  public static ListOpenWorkflowExecutionsRequest listOpenWorkflowExecutionsRequest(
      com.uber.cadence.entities.ListOpenWorkflowExecutionsRequest t) {
    if (t == null) {
      return null;
    }
    ListOpenWorkflowExecutionsRequest.Builder request =
        ListOpenWorkflowExecutionsRequest.newBuilder()
            .setDomain(t.getDomain())
            .setPageSize(t.getMaximumPageSize());
    if (t.getExecutionFilter() != null) {
      request.setExecutionFilter(workflowExecutionFilter(t.getExecutionFilter()));
    }
    if (t.getTypeFilter() != null) {
      request.setTypeFilter(workflowTypeFilter(t.getTypeFilter()));
    }
    if (t.getNextPageToken() != null) {
      request.setNextPageToken(arrayToByteString(t.getNextPageToken()));
    }
    if (t.getStartTimeFilter() != null) {
      request.setStartTimeFilter(startTimeFilter(t.getStartTimeFilter()));
    }
    return request.build();
  }

  public static RespondActivityTaskFailedByIDRequest respondActivityTaskFailedByIDRequest(
      com.uber.cadence.entities.RespondActivityTaskFailedByIDRequest failRequest) {
    if (failRequest == null) {
      return null;
    }
    RespondActivityTaskFailedByIDRequest.Builder request =
        RespondActivityTaskFailedByIDRequest.newBuilder()
            .setDomain(failRequest.getDomain())
            .setWorkflowExecution(
                TypeMapper.workflowRunPair(failRequest.getWorkflowID(), failRequest.getRunID()))
            .setActivityId(failRequest.getActivityID())
            .setFailure(failure(failRequest.getReason(), failRequest.getDetails()))
            .setIdentity(failRequest.getIdentity());
    return request.build();
  }

  public static RespondActivityTaskCompletedByIDRequest respondActivityTaskCompletedByIDRequest(
      com.uber.cadence.entities.RespondActivityTaskCompletedByIDRequest completeRequest) {
    if (completeRequest == null) {
      return null;
    }
    RespondActivityTaskCompletedByIDRequest.Builder request =
        RespondActivityTaskCompletedByIDRequest.newBuilder()
            .setDomain(completeRequest.getDomain())
            .setWorkflowExecution(
                TypeMapper.workflowRunPair(
                    completeRequest.getWorkflowID(), completeRequest.getRunID()))
            .setActivityId(completeRequest.getActivityID())
            .setResult(payload(completeRequest.getResult()))
            .setIdentity(completeRequest.getIdentity());
    return request.build();
  }

  public static RecordActivityTaskHeartbeatByIDRequest recordActivityTaskHeartbeatByIDRequest(
      com.uber.cadence.entities.RecordActivityTaskHeartbeatByIDRequest heartbeatRequest) {
    if (heartbeatRequest == null) {
      return null;
    }
    RecordActivityTaskHeartbeatByIDRequest.Builder request =
        RecordActivityTaskHeartbeatByIDRequest.newBuilder()
            .setDomain(heartbeatRequest.getDomain())
            .setWorkflowExecution(
                TypeMapper.workflowRunPair(
                    heartbeatRequest.getWorkflowID(), heartbeatRequest.getRunID()))
            .setActivityId(heartbeatRequest.getActivityID())
            .setDetails(payload(heartbeatRequest.getDetails()))
            .setIdentity(heartbeatRequest.getIdentity());
    return request.build();
  }

  public static RespondActivityTaskCanceledByIDRequest respondActivityTaskCanceledByIDRequest(
      com.uber.cadence.entities.RespondActivityTaskCanceledByIDRequest canceledRequest) {
    if (canceledRequest == null) {
      return null;
    }
    RespondActivityTaskCanceledByIDRequest.Builder request =
        RespondActivityTaskCanceledByIDRequest.newBuilder()
            .setDomain(canceledRequest.getDomain())
            .setWorkflowExecution(
                TypeMapper.workflowRunPair(
                    canceledRequest.getWorkflowID(), canceledRequest.getRunID()))
            .setActivityId(canceledRequest.getActivityID())
            .setDetails(payload(canceledRequest.getDetails()))
            .setIdentity(canceledRequest.getIdentity());
    return request.build();
  }

  public static GetTaskListsByDomainRequest getTaskListsByDomainRequest(
      com.uber.cadence.entities.GetTaskListsByDomainRequest domainRequest) {
    if (domainRequest == null) {
      return null;
    }
    GetTaskListsByDomainRequest.Builder request =
        GetTaskListsByDomainRequest.newBuilder().setDomain(domainRequest.getDomainName());
    return request.build();
  }

  public static RefreshWorkflowTasksRequest refreshWorkflowTasksRequest(
      com.uber.cadence.entities.RefreshWorkflowTasksRequest request) {
    if (request == null) {
      return null;
    }
    return RefreshWorkflowTasksRequest.newBuilder().setDomain(request.getDomain()).build();
  }
}
