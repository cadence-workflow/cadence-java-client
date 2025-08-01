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
package com.uber.cadence.internal.compatibility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;
import com.uber.cadence.api.v1.*;
import java.util.Map;

public final class ProtoObjects {
  public static final WorkflowType WORKFLOW_TYPE =
      WorkflowType.newBuilder().setName("workflowType").build();
  public static final ActivityType ACTIVITY_TYPE =
      ActivityType.newBuilder().setName("activityName").build();
  public static final TaskList TASK_LIST =
      TaskList.newBuilder().setName("taskList").setKind(TaskListKind.TASK_LIST_KIND_NORMAL).build();
  public static final TaskListMetadata TASK_LIST_METADATA =
      TaskListMetadata.newBuilder()
          .setMaxTasksPerSecond(DoubleValue.newBuilder().setValue(10.0).build())
          .build();
  public static final RetryPolicy RETRY_POLICY =
      RetryPolicy.newBuilder()
          .setInitialInterval(seconds(11))
          .setBackoffCoefficient(0.5)
          .setMaximumInterval(seconds(12))
          .setMaximumAttempts(13)
          .addNonRetryableErrorReasons("error")
          .setExpirationInterval(seconds(14))
          .build();
  public static final String WORKFLOW_ID = "workflowId";
  public static final WorkflowExecution WORKFLOW_EXECUTION =
      WorkflowExecution.newBuilder().setWorkflowId("workflowId").setRunId("runId").build();
  public static final WorkflowExecution EXTERNAL_WORKFLOW_EXECUTION =
      WorkflowExecution.newBuilder()
          .setWorkflowId("externalWorkflowId")
          .setRunId("externalRunId")
          .build();
  public static final WorkflowExecution PARENT_WORKFLOW_EXECUTION =
      WorkflowExecution.newBuilder()
          .setWorkflowId("parentWorkflowId")
          .setRunId("parentRunId")
          .build();

  public static final Failure FAILURE =
      Failure.newBuilder().setDetails(utf8("details")).setReason("reason").build();
  public static final StickyExecutionAttributes STICKY_EXECUTION_ATTRIBUTES =
      StickyExecutionAttributes.newBuilder()
          .setWorkerTaskList(TASK_LIST)
          .setScheduleToStartTimeout(seconds(1))
          .build();
  public static final WorkflowQuery WORKFLOW_QUERY =
      WorkflowQuery.newBuilder()
          .setQueryType("queryType")
          .setQueryArgs(payload("queryArgs"))
          .build();
  public static final WorkflowQueryResult WORKFLOW_QUERY_RESULT =
      WorkflowQueryResult.newBuilder()
          .setResultType(QueryResultType.QUERY_RESULT_TYPE_ANSWERED)
          .setAnswer(payload("answer"))
          .setErrorMessage("error")
          .build();
  public static final Header HEADER =
      Header.newBuilder().putFields("key", payload("value")).build();
  public static final Memo MEMO = Memo.newBuilder().putFields("memo", payload("memoValue")).build();
  public static final AutoConfigHint AUTO_CONFIG_HINT =
      AutoConfigHint.newBuilder().setEnableAutoConfig(true).setPollerWaitTimeInMs(100).build();
  public static final SearchAttributes SEARCH_ATTRIBUTES =
      SearchAttributes.newBuilder().putIndexedFields("search", payload("attributes")).build();
  public static final Map<String, String> DATA = ImmutableMap.of("dataKey", "dataValue");
  public static final ResetPointInfo RESET_POINT_INFO =
      ResetPointInfo.newBuilder()
          .setBinaryChecksum("binaryChecksum")
          .setRunId("runId")
          .setCreatedTime(timestampNanos(1))
          .setResettable(true)
          .setExpiringTime(timestampNanos(2))
          .setFirstDecisionCompletedId(3)
          .build();
  public static final ResetPoints RESET_POINTS =
      ResetPoints.newBuilder().addPoints(RESET_POINT_INFO).build();
  public static final ClusterReplicationConfiguration CLUSTER_REPLICATION_CONFIGURATION =
      ClusterReplicationConfiguration.newBuilder().setClusterName("cluster").build();
  public static final PollerInfo POLLER_INFO =
      PollerInfo.newBuilder()
          .setIdentity("identity")
          .setLastAccessTime(timestampNanos(1))
          .setRatePerSecond(2.0)
          .build();
  public static final TaskIDBlock TASK_ID_BLOCK =
      TaskIDBlock.newBuilder().setStartId(1).setEndId(2).build();
  public static final TaskListStatus TASK_LIST_STATUS =
      TaskListStatus.newBuilder()
          .setTaskIdBlock(TASK_ID_BLOCK)
          .setAckLevel(1)
          .setBacklogCountHint(2)
          .setReadLevel(3)
          .setRatePerSecond(4.0)
          .build();
  public static final WorkflowExecutionConfiguration WORKFLOW_EXECUTION_CONFIGURATION =
      WorkflowExecutionConfiguration.newBuilder()
          .setTaskList(TASK_LIST)
          .setExecutionStartToCloseTimeout(seconds(1))
          .setTaskStartToCloseTimeout(seconds(2))
          .build();
  public static final WorkflowExecutionInfo WORKFLOW_EXECUTION_INFO =
      WorkflowExecutionInfo.newBuilder()
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setType(WORKFLOW_TYPE)
          .setStartTime(timestampNanos(1))
          .setCloseTime(timestampNanos(2))
          .setCloseStatus(WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED)
          .setHistoryLength(3)
          .setParentExecutionInfo(
              ParentExecutionInfo.newBuilder()
                  .setDomainName("parentDomainName")
                  .setDomainId("parentDomainId")
                  .setWorkflowExecution(PARENT_WORKFLOW_EXECUTION)
                  .setInitiatedId(1)
                  .build())
          .setExecutionTime(timestampNanos(4))
          .setMemo(MEMO)
          .setSearchAttributes(SEARCH_ATTRIBUTES)
          .setAutoResetPoints(RESET_POINTS)
          .setTaskList(TASK_LIST.getName())
          .setIsCron(true)
          .build();
  public static final PendingActivityInfo PENDING_ACTIVITY_INFO =
      PendingActivityInfo.newBuilder()
          .setActivityId("activityId")
          .setActivityType(ACTIVITY_TYPE)
          .setState(PendingActivityState.PENDING_ACTIVITY_STATE_STARTED)
          .setHeartbeatDetails(payload("heartbeatDetails"))
          .setLastHeartbeatTime(timestampNanos(1))
          .setLastStartedTime(timestampNanos(2))
          .setAttempt(3)
          .setMaximumAttempts(4)
          .setScheduledTime(timestampNanos(5))
          .setExpirationTime(timestampNanos(6))
          .setLastWorkerIdentity("lastWorkerIdentity")
          .setLastFailure(
              Failure.newBuilder()
                  .setReason("lastFailureReason")
                  .setDetails(utf8("lastFailureDetails")))
          .build();
  public static final PendingChildExecutionInfo PENDING_CHILD_EXECUTION_INFO =
      PendingChildExecutionInfo.newBuilder()
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setWorkflowTypeName(WORKFLOW_TYPE.getName())
          .setInitiatedId(1)
          .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL)
          .build();
  public static final PendingDecisionInfo PENDING_DECISION_INFO =
      PendingDecisionInfo.newBuilder()
          .setState(PendingDecisionState.PENDING_DECISION_STATE_STARTED)
          .setScheduledTime(timestampNanos(1))
          .setStartedTime(timestampNanos(2))
          .setAttempt(3)
          .setOriginalScheduledTime(timestampNanos(4))
          .build();
  public static final SupportedClientVersions SUPPORTED_CLIENT_VERSIONS =
      SupportedClientVersions.newBuilder().setGoSdk("goSdk").setJavaSdk("javaSdk").build();
  public static final Map<String, IndexedValueType> INDEXED_VALUES =
      ImmutableMap.of(
          "STRING",
          IndexedValueType.INDEXED_VALUE_TYPE_STRING,
          "KEYWORD",
          IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD,
          "INT",
          IndexedValueType.INDEXED_VALUE_TYPE_INT,
          "DOUBLE",
          IndexedValueType.INDEXED_VALUE_TYPE_DOUBLE,
          "BOOL",
          IndexedValueType.INDEXED_VALUE_TYPE_BOOL,
          "DATETIME",
          IndexedValueType.INDEXED_VALUE_TYPE_DATETIME);
  public static final DataBlob DATA_BLOB =
      DataBlob.newBuilder()
          .setData(utf8("data"))
          .setEncodingType(EncodingType.ENCODING_TYPE_JSON)
          .build();
  public static final TaskListPartitionMetadata TASK_LIST_PARTITION_METADATA =
      TaskListPartitionMetadata.newBuilder()
          .setKey("key")
          .setOwnerHostName("ownerHostName")
          .build();
  public static final ActivityLocalDispatchInfo ACTIVITY_LOCAL_DISPATCH_INFO =
      ActivityLocalDispatchInfo.newBuilder()
          .setActivityId("activityId")
          .setScheduledTime(timestampNanos(1))
          .setStartedTime(timestampNanos(2))
          .setScheduledTimeOfThisAttempt(timestampNanos(3))
          .setTaskToken(utf8("taskToken"))
          .build();
  public static final BadBinaryInfo BAD_BINARY_INFO =
      BadBinaryInfo.newBuilder()
          .setReason("reason")
          .setOperator("operator")
          .setCreatedTime(timestampNanos(3))
          .build();
  public static final BadBinaries BAD_BINARIES =
      BadBinaries.newBuilder().putBinaries("badBinaryKey", BAD_BINARY_INFO).build();
  public static final Domain DOMAIN =
      Domain.newBuilder()
          .setId("uuid")
          .setName("domain")
          .setStatus(DomainStatus.DOMAIN_STATUS_DEPRECATED)
          .setDescription("description")
          .setOwnerEmail("email")
          .putAllData(DATA)
          .setWorkflowExecutionRetentionPeriod(days(2))
          .setBadBinaries(BAD_BINARIES)
          .setHistoryArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_ENABLED)
          .setHistoryArchivalUri("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_DISABLED)
          .setVisibilityArchivalUri("visibilityArchivalUri")
          .setActiveClusterName("activeCluster")
          .addClusters(CLUSTER_REPLICATION_CONFIGURATION)
          .setFailoverVersion(1)
          .setIsGlobalDomain(true)
          .build();
  public static final WorkerVersionInfo WORKER_VERSION_INFO =
      WorkerVersionInfo.newBuilder().setFeatureVersion("featureVersion").setImpl("impl").build();
  public static final StartTimeFilter START_TIME_FILTER =
      StartTimeFilter.newBuilder()
          .setEarliestTime(timestampNanos(2))
          .setLatestTime(timestampNanos(3))
          .build();
  public static final WorkflowExecutionFilter WORKFLOW_EXECUTION_FILTER =
      WorkflowExecutionFilter.newBuilder()
          .setWorkflowId(WORKFLOW_EXECUTION.getWorkflowId())
          .setRunId(WORKFLOW_EXECUTION.getRunId())
          .build();
  public static final WorkflowTypeFilter WORKFLOW_TYPE_FILTER =
      WorkflowTypeFilter.newBuilder().setName(WORKFLOW_TYPE.getName()).build();
  public static final StatusFilter STATUS_FILTER =
      StatusFilter.newBuilder()
          .setStatus(WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED)
          .build();

  public static Decision DECISION_SCHEDULE_ACTIVITY_TASK =
      Decision.newBuilder()
          .setScheduleActivityTaskDecisionAttributes(
              ScheduleActivityTaskDecisionAttributes.newBuilder()
                  .setActivityId("activityId")
                  .setActivityType(ACTIVITY_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setScheduleToCloseTimeout(seconds(1))
                  .setScheduleToStartTimeout(seconds(2))
                  .setStartToCloseTimeout(seconds(3))
                  .setHeartbeatTimeout(seconds(4))
                  .setHeader(HEADER)
                  .setRequestLocalDispatch(true)
                  .setRetryPolicy(RETRY_POLICY)
                  .setDomain("domain"))
          .build();
  public static Decision DECISION_REQUEST_CANCEL_ACTIVITY_TASK =
      Decision.newBuilder()
          .setRequestCancelActivityTaskDecisionAttributes(
              RequestCancelActivityTaskDecisionAttributes.newBuilder().setActivityId("activityId"))
          .build();
  public static Decision DECISION_START_TIMER =
      Decision.newBuilder()
          .setStartTimerDecisionAttributes(
              StartTimerDecisionAttributes.newBuilder()
                  .setTimerId("timerId")
                  .setStartToFireTimeout(seconds(2)))
          .build();
  public static Decision DECISION_COMPLETE_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setCompleteWorkflowExecutionDecisionAttributes(
              CompleteWorkflowExecutionDecisionAttributes.newBuilder().setResult(payload("result")))
          .build();
  public static Decision DECISION_FAIL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setFailWorkflowExecutionDecisionAttributes(
              FailWorkflowExecutionDecisionAttributes.newBuilder().setFailure(FAILURE))
          .build();
  public static Decision DECISION_CANCEL_TIMER =
      Decision.newBuilder()
          .setCancelTimerDecisionAttributes(
              CancelTimerDecisionAttributes.newBuilder().setTimerId("timerId"))
          .build();
  public static Decision DECISION_CANCEL_WORKFLOW =
      Decision.newBuilder()
          .setCancelWorkflowExecutionDecisionAttributes(
              CancelWorkflowExecutionDecisionAttributes.newBuilder().setDetails(payload("details")))
          .build();
  public static Decision DECISION_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setRequestCancelExternalWorkflowExecutionDecisionAttributes(
              RequestCancelExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowExecution(WORKFLOW_EXECUTION)
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")))
          .build();
  public static Decision DECISION_CONTINUE_AS_NEW_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setContinueAsNewWorkflowExecutionDecisionAttributes(
              ContinueAsNewWorkflowExecutionDecisionAttributes.newBuilder()
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setExecutionStartToCloseTimeout(seconds(1))
                  .setTaskStartToCloseTimeout(seconds(2))
                  .setBackoffStartInterval(seconds(3))
                  .setInitiator(ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_DECIDER)
                  .setFailure(FAILURE)
                  .setLastCompletionResult(payload("lastCompletionResult"))
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron"))
          .build();
  public static Decision DECISION_START_CHILD_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setStartChildWorkflowExecutionDecisionAttributes(
              StartChildWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowId("workflowId")
                  .setWorkflowType(WORKFLOW_TYPE)
                  .setTaskList(TASK_LIST)
                  .setInput(payload("input"))
                  .setExecutionStartToCloseTimeout(seconds(1))
                  .setTaskStartToCloseTimeout(seconds(2))
                  .setHeader(HEADER)
                  .setMemo(MEMO)
                  .setSearchAttributes(SEARCH_ATTRIBUTES)
                  .setRetryPolicy(RETRY_POLICY)
                  .setCronSchedule("cron")
                  .setControl(utf8("control"))
                  .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                  .setWorkflowIdReusePolicy(
                      WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE))
          .build();
  public static Decision DECISION_SIGNAL_EXTERNAL_WORKFLOW_EXECUTION =
      Decision.newBuilder()
          .setSignalExternalWorkflowExecutionDecisionAttributes(
              SignalExternalWorkflowExecutionDecisionAttributes.newBuilder()
                  .setDomain("domain")
                  .setWorkflowExecution(WORKFLOW_EXECUTION)
                  .setSignalName("signalName")
                  .setInput(payload("input"))
                  .setChildWorkflowOnly(true)
                  .setControl(utf8("control")))
          .build();
  public static Decision DECISION_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES =
      Decision.newBuilder()
          .setUpsertWorkflowSearchAttributesDecisionAttributes(
              UpsertWorkflowSearchAttributesDecisionAttributes.newBuilder()
                  .setSearchAttributes(SEARCH_ATTRIBUTES))
          .build();
  public static Decision DECISION_RECORD_MARKER =
      Decision.newBuilder()
          .setRecordMarkerDecisionAttributes(
              RecordMarkerDecisionAttributes.newBuilder()
                  .setMarkerName("markerName")
                  .setDetails(payload("details"))
                  .setHeader(HEADER))
          .build();

  public static final WorkflowExecutionStartedEventAttributes
      WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES =
          WorkflowExecutionStartedEventAttributes.newBuilder()
              .setWorkflowType(WORKFLOW_TYPE)
              .setParentExecutionInfo(
                  ParentExecutionInfo.newBuilder()
                      .setDomainName("parentDomainName")
                      .setWorkflowExecution(PARENT_WORKFLOW_EXECUTION)
                      .setInitiatedId(1)
                      .build())
              .setTaskList(TASK_LIST)
              .setInput(payload("input"))
              .setExecutionStartToCloseTimeout(seconds(2))
              .setTaskStartToCloseTimeout(seconds(3))
              .setContinuedExecutionRunId("continuedExecutionRunId")
              .setInitiator(ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY)
              .setContinuedFailure(
                  Failure.newBuilder()
                      .setReason("continuedFailureReason")
                      .setDetails(utf8("continuedFailureDetails"))
                      .build())
              .setLastCompletionResult(payload("lastCompletionResult"))
              .setOriginalExecutionRunId("originalExecutionRunId")
              .setIdentity("identity")
              .setFirstExecutionRunId("firstExecutionRunId")
              .setRetryPolicy(RETRY_POLICY)
              .setAttempt(4)
              .setExpirationTime(timestampNanos(5))
              .setCronSchedule("cronSchedule")
              .setFirstDecisionTaskBackoff(seconds(6))
              .setMemo(MEMO)
              .setSearchAttributes(SEARCH_ATTRIBUTES)
              .setPrevAutoResetPoints(RESET_POINTS)
              .setHeader(HEADER)
              .build();

  public static final WorkflowExecutionCompletedEventAttributes
      WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES =
          WorkflowExecutionCompletedEventAttributes.newBuilder()
              .setResult(payload("result"))
              .setDecisionTaskCompletedEventId(1)
              .build();

  public static final WorkflowExecutionFailedEventAttributes
      WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES =
          WorkflowExecutionFailedEventAttributes.newBuilder()
              .setFailure(FAILURE)
              .setDecisionTaskCompletedEventId(1)
              .build();

  public static final WorkflowExecutionTimedOutEventAttributes
      WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES =
          WorkflowExecutionTimedOutEventAttributes.newBuilder()
              .setTimeoutType(TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_CLOSE)
              .build();

  public static final DecisionTaskScheduledEventAttributes
      DECISION_TASK_SCHEDULED_EVENT_ATTRIBUTES =
          DecisionTaskScheduledEventAttributes.newBuilder()
              .setTaskList(TASK_LIST)
              .setStartToCloseTimeout(seconds(1))
              .setAttempt(2)
              .build();

  public static final DecisionTaskStartedEventAttributes DECISION_TASK_STARTED_EVENT_ATTRIBUTES =
      DecisionTaskStartedEventAttributes.newBuilder()
          .setScheduledEventId(1)
          .setIdentity("identity")
          .setRequestId("requestId")
          .build();

  public static final DecisionTaskCompletedEventAttributes
      DECISION_TASK_COMPLETED_EVENT_ATTRIBUTES =
          DecisionTaskCompletedEventAttributes.newBuilder()
              .setScheduledEventId(1)
              .setStartedEventId(2)
              .setIdentity("identity")
              .setBinaryChecksum("binaryChecksum")
              .setExecutionContext(utf8("executionContext"))
              .build();

  public static final DecisionTaskTimedOutEventAttributes DECISION_TASK_TIMED_OUT_EVENT_ATTRIBUTES =
      DecisionTaskTimedOutEventAttributes.newBuilder()
          .setScheduledEventId(1)
          .setStartedEventId(2)
          .setTimeoutType(TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_CLOSE)
          .setBaseRunId("baseRunId")
          .setNewRunId("newRunId")
          .setForkEventVersion(3)
          .setReason("reason")
          .setCause(DecisionTaskTimedOutCause.DECISION_TASK_TIMED_OUT_CAUSE_RESET)
          .build();

  public static final DecisionTaskFailedEventAttributes DECISION_TASK_FAILED_EVENT_ATTRIBUTES =
      DecisionTaskFailedEventAttributes.newBuilder()
          .setScheduledEventId(1)
          .setStartedEventId(2)
          .setCause(DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_BINARY)
          .setFailure(FAILURE)
          .setIdentity("identity")
          .setBaseRunId("baseRun")
          .setNewRunId("newRun")
          .setForkEventVersion(3)
          .setBinaryChecksum("binaryChecksum")
          .build();

  public static final ActivityTaskScheduledEventAttributes
      ACTIVITY_TASK_SCHEDULED_EVENT_ATTRIBUTES =
          ActivityTaskScheduledEventAttributes.newBuilder()
              .setActivityId("activityId")
              .setActivityType(ACTIVITY_TYPE)
              .setDomain("domain")
              .setTaskList(TASK_LIST)
              .setInput(payload("input"))
              .setScheduleToCloseTimeout(seconds(1))
              .setScheduleToStartTimeout(seconds(2))
              .setStartToCloseTimeout(seconds(3))
              .setHeartbeatTimeout(seconds(4))
              .setDecisionTaskCompletedEventId(5)
              .setRetryPolicy(RETRY_POLICY)
              .setHeader(HEADER)
              .build();

  public static final ActivityTaskStartedEventAttributes ACTIVITY_TASK_STARTED_EVENT_ATTRIBUTES =
      ActivityTaskStartedEventAttributes.newBuilder()
          .setScheduledEventId(1)
          .setIdentity("identity")
          .setRequestId("requestId")
          .setAttempt(2)
          .setLastFailure(
              Failure.newBuilder()
                  .setReason("failureReason")
                  .setDetails(utf8("failureDetails"))
                  .build())
          .build();

  public static final ActivityTaskCompletedEventAttributes
      ACTIVITY_TASK_COMPLETED_EVENT_ATTRIBUTES =
          ActivityTaskCompletedEventAttributes.newBuilder()
              .setResult(payload("result"))
              .setScheduledEventId(1)
              .setStartedEventId(2)
              .setIdentity("identity")
              .build();

  public static final ActivityTaskFailedEventAttributes ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES =
      ActivityTaskFailedEventAttributes.newBuilder()
          .setFailure(FAILURE)
          .setScheduledEventId(1)
          .setStartedEventId(2)
          .setIdentity("identity")
          .build();

  public static final ActivityTaskTimedOutEventAttributes ACTIVITY_TASK_TIMED_OUT_EVENT_ATTRIBUTES =
      ActivityTaskTimedOutEventAttributes.newBuilder()
          .setDetails(payload("details"))
          .setScheduledEventId(1)
          .setStartedEventId(2)
          .setTimeoutType(TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_CLOSE)
          .setLastFailure(
              Failure.newBuilder()
                  .setReason("failureReason")
                  .setDetails(utf8("failureDetails"))
                  .build())
          .build();

  public static final ActivityTaskCancelRequestedEventAttributes
      ACTIVITY_TASK_CANCEL_REQUESTED_EVENT_ATTRIBUTES =
          ActivityTaskCancelRequestedEventAttributes.newBuilder()
              .setActivityId("activityId")
              .setDecisionTaskCompletedEventId(1)
              .build();

  public static final ActivityTaskCanceledEventAttributes ACTIVITY_TASK_CANCELED_EVENT_ATTRIBUTES =
      ActivityTaskCanceledEventAttributes.newBuilder()
          .setDetails(payload("details"))
          .setLatestCancelRequestedEventId(1)
          .setScheduledEventId(2)
          .setStartedEventId(3)
          .setIdentity("identity")
          .build();

  public static final RequestCancelActivityTaskFailedEventAttributes
      REQUEST_CANCEL_ACTIVITY_TASK_FAILED_EVENT_ATTRIBUTES =
          RequestCancelActivityTaskFailedEventAttributes.newBuilder()
              .setActivityId("activityId")
              .setCause("cause")
              .setDecisionTaskCompletedEventId(1)
              .build();

  public static final MarkerRecordedEventAttributes MARKER_RECORDED_EVENT_ATTRIBUTES =
      MarkerRecordedEventAttributes.newBuilder()
          .setMarkerName("markerName")
          .setDetails(payload("details"))
          .setDecisionTaskCompletedEventId(1)
          .setHeader(HEADER)
          .build();

  public static final TimerCanceledEventAttributes TIMER_CANCELED_EVENT_ATTRIBUTES =
      TimerCanceledEventAttributes.newBuilder()
          .setTimerId("timerId")
          .setStartedEventId(1)
          .setDecisionTaskCompletedEventId(2)
          .setIdentity("identity")
          .build();

  public static final CancelTimerFailedEventAttributes CANCEL_TIMER_FAILED_EVENT_ATTRIBUTES =
      CancelTimerFailedEventAttributes.newBuilder()
          .setTimerId("timerId")
          .setCause("cause")
          .setDecisionTaskCompletedEventId(1)
          .setIdentity("identity")
          .build();

  public static final TimerFiredEventAttributes TIMER_FIRED_EVENT_ATTRIBUTES =
      TimerFiredEventAttributes.newBuilder().setTimerId("timerId").setStartedEventId(1).build();

  public static final TimerStartedEventAttributes TIMER_STARTED_EVENT_ATTRIBUTES =
      TimerStartedEventAttributes.newBuilder()
          .setTimerId("timerId")
          .setStartToFireTimeout(seconds(1))
          .setDecisionTaskCompletedEventId(2)
          .build();

  public static final UpsertWorkflowSearchAttributesEventAttributes
      UPSERT_WORKFLOW_SEARCH_ATTRIBUTES_EVENT_ATTRIBUTES =
          UpsertWorkflowSearchAttributesEventAttributes.newBuilder()
              .setDecisionTaskCompletedEventId(1)
              .setSearchAttributes(SEARCH_ATTRIBUTES)
              .build();

  public static final StartChildWorkflowExecutionInitiatedEventAttributes
      START_CHILD_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES =
          StartChildWorkflowExecutionInitiatedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowId(WORKFLOW_ID)
              .setWorkflowType(WORKFLOW_TYPE)
              .setTaskList(TASK_LIST)
              .setInput(payload("input"))
              .setExecutionStartToCloseTimeout(seconds(1))
              .setTaskStartToCloseTimeout(seconds(2))
              .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL)
              .setControl(utf8("control"))
              .setDecisionTaskCompletedEventId(3)
              .setWorkflowIdReusePolicy(
                  WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
              .setRetryPolicy(RETRY_POLICY)
              .setCronSchedule("cron")
              .setHeader(HEADER)
              .setMemo(MEMO)
              .setSearchAttributes(SEARCH_ATTRIBUTES)
              .setDelayStart(seconds(4))
              .setJitterStart(seconds(5))
              .setFirstRunAt(timestampNanos(123456789))
              .build();

  public static final StartChildWorkflowExecutionFailedEventAttributes
      START_CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES =
          StartChildWorkflowExecutionFailedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowId(WORKFLOW_ID)
              .setWorkflowType(WORKFLOW_TYPE)
              .setCause(
                  ChildWorkflowExecutionFailedCause
                      .CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_RUNNING)
              .setControl(utf8("control"))
              .setInitiatedEventId(1)
              .setDecisionTaskCompletedEventId(2)
              .build();

  public static final ChildWorkflowExecutionCanceledEventAttributes
      CHILD_WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionCanceledEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setStartedEventId(2)
              .setDetails(payload("details"))
              .build();

  public static final ChildWorkflowExecutionCompletedEventAttributes
      CHILD_WORKFLOW_EXECUTION_COMPLETED_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionCompletedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setStartedEventId(2)
              .setResult(payload("result"))
              .build();

  public static final ChildWorkflowExecutionFailedEventAttributes
      CHILD_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionFailedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setStartedEventId(2)
              .setFailure(FAILURE)
              .build();

  public static final ChildWorkflowExecutionStartedEventAttributes
      CHILD_WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionStartedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setHeader(HEADER)
              .build();

  public static final ChildWorkflowExecutionTerminatedEventAttributes
      CHILD_WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionTerminatedEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setStartedEventId(2)
              .build();

  public static final ChildWorkflowExecutionTimedOutEventAttributes
      CHILD_WORKFLOW_EXECUTION_TIMED_OUT_EVENT_ATTRIBUTES =
          ChildWorkflowExecutionTimedOutEventAttributes.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setWorkflowType(WORKFLOW_TYPE)
              .setInitiatedEventId(1)
              .setStartedEventId(2)
              .setTimeoutType(TimeoutType.TIMEOUT_TYPE_SCHEDULE_TO_CLOSE)
              .build();

  public static final WorkflowExecutionTerminatedEventAttributes
      WORKFLOW_EXECUTION_TERMINATED_EVENT_ATTRIBUTES =
          WorkflowExecutionTerminatedEventAttributes.newBuilder()
              .setReason("reason")
              .setDetails(payload("details"))
              .setIdentity("identity")
              .build();

  public static final ExternalExecutionInfo EXTERNAL_WORKFLOW_EXECUTION_INFO =
      ExternalExecutionInfo.newBuilder()
          .setInitiatedId(1)
          .setWorkflowExecution(EXTERNAL_WORKFLOW_EXECUTION)
          .build();

  public static final WorkflowExecutionCancelRequestedEventAttributes
      WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES =
          WorkflowExecutionCancelRequestedEventAttributes.newBuilder()
              .setCause("cause")
              .setExternalExecutionInfo(
                  ExternalExecutionInfo.newBuilder()
                      .setInitiatedId(1)
                      .setWorkflowExecution(WORKFLOW_EXECUTION)
                      .build())
              .setIdentity("identity")
              .build();

  public static final WorkflowExecutionCanceledEventAttributes
      WORKFLOW_EXECUTION_CANCELED_EVENT_ATTRIBUTES =
          WorkflowExecutionCanceledEventAttributes.newBuilder()
              .setDecisionTaskCompletedEventId(1)
              .setDetails(payload("details"))
              .build();

  public static final RequestCancelExternalWorkflowExecutionInitiatedEventAttributes
      REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES =
          RequestCancelExternalWorkflowExecutionInitiatedEventAttributes.newBuilder()
              .setDecisionTaskCompletedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setControl(utf8("control"))
              .setChildWorkflowOnly(true)
              .build();

  public static final RequestCancelExternalWorkflowExecutionFailedEventAttributes
      REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES =
          RequestCancelExternalWorkflowExecutionFailedEventAttributes.newBuilder()
              .setCause(
                  CancelExternalWorkflowExecutionFailedCause
                      .CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED)
              .setDecisionTaskCompletedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setInitiatedEventId(2)
              .setControl(utf8("control"))
              .build();

  public static final ExternalWorkflowExecutionCancelRequestedEventAttributes
      EXTERNAL_WORKFLOW_EXECUTION_CANCEL_REQUESTED_EVENT_ATTRIBUTES =
          ExternalWorkflowExecutionCancelRequestedEventAttributes.newBuilder()
              .setInitiatedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .build();

  public static final WorkflowExecutionContinuedAsNewEventAttributes
      WORKFLOW_EXECUTION_CONTINUED_AS_NEW_EVENT_ATTRIBUTES =
          WorkflowExecutionContinuedAsNewEventAttributes.newBuilder()
              .setNewExecutionRunId("newRunId")
              .setWorkflowType(WORKFLOW_TYPE)
              .setTaskList(TASK_LIST)
              .setInput(payload("input"))
              .setExecutionStartToCloseTimeout(seconds(1))
              .setTaskStartToCloseTimeout(seconds(2))
              .setDecisionTaskCompletedEventId(3)
              .setBackoffStartInterval(seconds(4))
              .setInitiator(ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY)
              .setFailure(
                  Failure.newBuilder()
                      .setReason("failureReason")
                      .setDetails(utf8("failureDetails"))
                      .build())
              .setLastCompletionResult(payload("lastCompletionResult"))
              .setHeader(HEADER)
              .setMemo(MEMO)
              .setSearchAttributes(SEARCH_ATTRIBUTES)
              .build();

  public static final SignalExternalWorkflowExecutionInitiatedEventAttributes
      SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_INITIATED_EVENT_ATTRIBUTES =
          SignalExternalWorkflowExecutionInitiatedEventAttributes.newBuilder()
              .setDecisionTaskCompletedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setSignalName("signalName")
              .setInput(payload("input"))
              .setControl(utf8("control"))
              .setChildWorkflowOnly(true)
              .build();

  public static final SignalExternalWorkflowExecutionFailedEventAttributes
      SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_EVENT_ATTRIBUTES =
          SignalExternalWorkflowExecutionFailedEventAttributes.newBuilder()
              .setCause(
                  SignalExternalWorkflowExecutionFailedCause
                      .SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED)
              .setDecisionTaskCompletedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setInitiatedEventId(2)
              .setControl(utf8("control"))
              .build();

  public static final WorkflowExecutionSignaledEventAttributes
      WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES =
          WorkflowExecutionSignaledEventAttributes.newBuilder()
              .setSignalName("signalName")
              .setInput(payload("input"))
              .setIdentity("identity")
              .build();

  public static final ExternalWorkflowExecutionSignaledEventAttributes
      EXTERNAL_WORKFLOW_EXECUTION_SIGNALED_EVENT_ATTRIBUTES =
          ExternalWorkflowExecutionSignaledEventAttributes.newBuilder()
              .setInitiatedEventId(1)
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setControl(utf8("control"))
              .build();

  public static final HistoryEvent HISTORY_EVENT =
      HistoryEvent.newBuilder()
          .setEventId(1)
          .setEventTime(timestampNanos(2))
          .setVersion(3)
          .setTaskId(4)
          .setWorkflowExecutionStartedEventAttributes(WORKFLOW_EXECUTION_STARTED_EVENT_ATTRIBUTES)
          .build();

  public static final History HISTORY = History.newBuilder().addEvents(HISTORY_EVENT).build();

  public static final CountWorkflowExecutionsRequest COUNT_WORKFLOW_EXECUTIONS_REQUEST =
      CountWorkflowExecutionsRequest.newBuilder().setDomain("domain").setQuery("query").build();
  public static final DescribeTaskListRequest DESCRIBE_TASK_LIST_REQUEST =
      DescribeTaskListRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListType(TaskListType.TASK_LIST_TYPE_ACTIVITY)
          .setIncludeTaskListStatus(true)
          .build();
  public static final ListArchivedWorkflowExecutionsRequest
      LIST_ARCHIVED_WORKFLOW_EXECUTIONS_REQUEST =
          ListArchivedWorkflowExecutionsRequest.newBuilder()
              .setDomain("domain")
              .setPageSize(1)
              .setNextPageToken(utf8("pageToken"))
              .setQuery("query")
              .build();
  public static final RequestCancelWorkflowExecutionRequest
      REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST =
          RequestCancelWorkflowExecutionRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setRequestId("requestId")
              .setIdentity("identity")
              .build();
  public static final RequestCancelWorkflowExecutionRequest
      REQUEST_CANCEL_WORKFLOW_EXECUTION_REQUEST_FULL =
          RequestCancelWorkflowExecutionRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setRequestId("requestId")
              .setIdentity("identity")
              .setFirstExecutionRunId("firstExecutionRunID")
              .setCause("cancel cause")
              .build();
  public static final ResetStickyTaskListRequest RESET_STICKY_TASK_LIST_REQUEST =
      ResetStickyTaskListRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .build();
  public static final ResetWorkflowExecutionRequest RESET_WORKFLOW_EXECUTION_REQUEST =
      ResetWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDecisionFinishEventId(1)
          .setRequestId("requestId")
          .setSkipSignalReapply(true)
          .build();
  public static final RespondActivityTaskCanceledByIDRequest
      RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_REQUEST =
          RespondActivityTaskCanceledByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setDetails(payload("details"))
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskCanceledRequest RESPOND_ACTIVITY_TASK_CANCELED_REQUEST =
      RespondActivityTaskCanceledRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .setDetails(payload("details"))
          .build();
  public static final RespondActivityTaskCompletedByIDRequest
      RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_REQUEST =
          RespondActivityTaskCompletedByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setResult(payload("result"))
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskCompletedRequest RESPOND_ACTIVITY_TASK_COMPLETED_REQUEST =
      RespondActivityTaskCompletedRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .setResult(payload("result"))
          .build();
  public static final RespondActivityTaskFailedByIDRequest
      RESPOND_ACTIVITY_TASK_FAILED_BY_ID_REQUEST =
          RespondActivityTaskFailedByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setFailure(FAILURE)
              .setIdentity("identity")
              .build();
  public static final RespondActivityTaskFailedRequest RESPOND_ACTIVITY_TASK_FAILED_REQUEST =
      RespondActivityTaskFailedRequest.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setFailure(FAILURE)
          .setIdentity("identity")
          .build();
  public static final RespondDecisionTaskCompletedRequest RESPOND_DECISION_TASK_COMPLETED_REQUEST =
      RespondDecisionTaskCompletedRequest.newBuilder()
          .addAllDecisions(ImmutableList.of(DECISION_COMPLETE_WORKFLOW_EXECUTION))
          .setStickyAttributes(STICKY_EXECUTION_ATTRIBUTES)
          .setReturnNewDecisionTask(true)
          .setForceCreateNewDecisionTask(false)
          .putAllQueryResults(ImmutableMap.of("query", WORKFLOW_QUERY_RESULT))
          .setExecutionContext(utf8("executionContext"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();
  public static final RespondDecisionTaskFailedRequest RESPOND_DECISION_TASK_FAILED_REQUEST =
      RespondDecisionTaskFailedRequest.newBuilder()
          .setCause(DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_BINARY)
          .setDetails(payload("details"))
          .setBinaryChecksum("binaryChecksum")
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();
  public static final RespondQueryTaskCompletedRequest RESPOND_QUERY_TASK_COMPLETED_REQUEST =
      RespondQueryTaskCompletedRequest.newBuilder()
          .setResult(
              WorkflowQueryResult.newBuilder()
                  .setResultType(QueryResultType.QUERY_RESULT_TYPE_ANSWERED)
                  .setAnswer(payload("queryResult"))
                  .setErrorMessage("errorMessage"))
          .setWorkerVersionInfo(WORKER_VERSION_INFO)
          .setTaskToken(utf8("taskToken"))
          .build();
  public static final ScanWorkflowExecutionsRequest SCAN_WORKFLOW_EXECUTIONS_REQUEST =
      ScanWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .setQuery("query")
          .build();
  public static final DescribeWorkflowExecutionRequest DESCRIBE_WORKFLOW_EXECUTION_REQUEST =
      DescribeWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setQueryConsistencyLevel(QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG)
          .build();

  public static final GetWorkflowExecutionHistoryRequest GET_WORKFLOW_EXECUTION_HISTORY_REQUEST =
      GetWorkflowExecutionHistoryRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setPageSize(1)
          .setWaitForNewEvent(true)
          .setHistoryEventFilterType(EventFilterType.EVENT_FILTER_TYPE_CLOSE_EVENT)
          .setSkipArchival(true)
          .setNextPageToken(utf8("nextPageToken"))
          .setQueryConsistencyLevel(QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG)
          .build();

  public static final StartWorkflowExecutionRequest START_WORKFLOW_EXECUTION =
      StartWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowId("workflowId")
          .setWorkflowType(WorkflowType.newBuilder().setName("workflowType"))
          .setTaskList(TASK_LIST)
          .setInput(payload("input"))
          .setExecutionStartToCloseTimeout(seconds(1))
          .setTaskStartToCloseTimeout(seconds(2))
          .setIdentity("identity")
          .setRequestId("requestId")
          .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
          .setRetryPolicy(RETRY_POLICY)
          .setCronSchedule("cronSchedule")
          .setMemo(MEMO)
          .setSearchAttributes(SEARCH_ATTRIBUTES)
          .setHeader(HEADER)
          .setDelayStart(seconds(3))
          .setJitterStart(seconds(0))
          .setFirstRunAt(timestampNanos(123456789))
          .build();

  public static final SignalWithStartWorkflowExecutionRequest SIGNAL_WITH_START_WORKFLOW_EXECUTION =
      com.uber.cadence.api.v1.SignalWithStartWorkflowExecutionRequest.newBuilder()
          .setStartRequest(START_WORKFLOW_EXECUTION)
          .setSignalInput(payload("signalInput"))
          .setSignalName("signalName")
          .setControl(utf8("control"))
          .build();

  public static final StartWorkflowExecutionAsyncRequest START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
      StartWorkflowExecutionAsyncRequest.newBuilder().setRequest(START_WORKFLOW_EXECUTION).build();

  public static final SignalWithStartWorkflowExecutionAsyncRequest
      SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_REQUEST =
          SignalWithStartWorkflowExecutionAsyncRequest.newBuilder()
              .setRequest(SIGNAL_WITH_START_WORKFLOW_EXECUTION)
              .build();

  public static final SignalWorkflowExecutionRequest SIGNAL_WORKFLOW_EXECUTION_REQUEST =
      SignalWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setSignalName("signalName")
          .setSignalInput(payload("input"))
          .setRequestId("requestId")
          .setControl(utf8("control"))
          .setIdentity("identity")
          .build();

  public static final TerminateWorkflowExecutionRequest TERMINATE_WORKFLOW_EXECUTION_REQUEST =
      TerminateWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDetails(payload("details"))
          .setIdentity("identity")
          .build();

  public static final TerminateWorkflowExecutionRequest TERMINATE_WORKFLOW_EXECUTION_REQUEST_FULL =
      TerminateWorkflowExecutionRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setReason("reason")
          .setDetails(payload("details"))
          .setIdentity("identity")
          .setFirstExecutionRunId("firstExecutionRunID")
          .build();

  public static final DeprecateDomainRequest DEPRECATE_DOMAIN_REQUEST =
      DeprecateDomainRequest.newBuilder()
          .setName("domain")
          .setSecurityToken("securityToken")
          .build();

  public static final ListWorkflowExecutionsRequest LIST_WORKFLOW_EXECUTIONS_REQUEST =
      ListWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setQuery("query")
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .build();

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_ID_REQUEST =
      DescribeDomainRequest.newBuilder().setId("uuid").build();

  public static final DescribeDomainRequest DESCRIBE_DOMAIN_BY_NAME_REQUEST =
      DescribeDomainRequest.newBuilder().setName("name").build();

  public static final ListDomainsRequest LIST_DOMAINS_REQUEST =
      ListDomainsRequest.newBuilder()
          .setPageSize(1)
          .setNextPageToken(utf8("nextPageToken"))
          .build();

  public static final ListTaskListPartitionsRequest LIST_TASK_LIST_PARTITIONS_REQUEST =
      ListTaskListPartitionsRequest.newBuilder().setDomain("domain").setTaskList(TASK_LIST).build();

  public static final PollForActivityTaskRequest POLL_FOR_ACTIVITY_TASK_REQUEST =
      PollForActivityTaskRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setTaskListMetadata(TASK_LIST_METADATA)
          .setIdentity("identity")
          .build();
  public static final PollForDecisionTaskRequest POLL_FOR_DECISION_TASK_REQUEST =
      PollForDecisionTaskRequest.newBuilder()
          .setDomain("domain")
          .setTaskList(TASK_LIST)
          .setBinaryChecksum("binaryChecksum")
          .setIdentity("identity")
          .build();
  public static final QueryWorkflowRequest QUERY_WORKFLOW_REQUEST =
      QueryWorkflowRequest.newBuilder()
          .setDomain("domain")
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setQuery(WORKFLOW_QUERY)
          .setQueryRejectCondition(
              QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY)
          .setQueryConsistencyLevel(QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG)
          .build();

  public static final RecordActivityTaskHeartbeatByIDRequest
      RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_REQUEST =
          RecordActivityTaskHeartbeatByIDRequest.newBuilder()
              .setDomain("domain")
              .setWorkflowExecution(WORKFLOW_EXECUTION)
              .setActivityId("activityId")
              .setDetails(payload("details"))
              .setIdentity("identity")
              .build();

  public static final RecordActivityTaskHeartbeatRequest RECORD_ACTIVITY_TASK_HEARTBEAT_REQUEST =
      RecordActivityTaskHeartbeatRequest.newBuilder()
          .setDetails(payload("details"))
          .setTaskToken(utf8("taskToken"))
          .setIdentity("identity")
          .build();

  public static final RegisterDomainRequest REGISTER_DOMAIN_REQUEST =
      RegisterDomainRequest.newBuilder()
          .setName("domain")
          .setDescription("description")
          .setOwnerEmail("ownerEmail")
          .setWorkflowExecutionRetentionPeriod(days(1))
          .addAllClusters(ImmutableList.of(CLUSTER_REPLICATION_CONFIGURATION))
          .setActiveClusterName("activeCluster")
          .putAllData(DATA)
          .setSecurityToken("securityToken")
          .setIsGlobalDomain(true)
          .setHistoryArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_ENABLED)
          .setHistoryArchivalUri("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_DISABLED)
          .setVisibilityArchivalUri("visibilityArchivalUri")
          .build();

  public static final UpdateDomainRequest UPDATE_DOMAIN_REQUEST =
      UpdateDomainRequest.newBuilder()
          .setName("domain")
          .setSecurityToken("securityToken")
          .setDescription("description")
          .setOwnerEmail("ownerEmail")
          .setWorkflowExecutionRetentionPeriod(days(2))
          .setBadBinaries(
              BadBinaries.newBuilder()
                  .putBinaries(
                      "badBinaryKey",
                      BadBinaryInfo.newBuilder()
                          .setReason("reason")
                          .setOperator("operator")
                          .setCreatedTime(timestampNanos(3))
                          .build()))
          .setHistoryArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_ENABLED)
          .setHistoryArchivalUri("historyArchivalUri")
          .setVisibilityArchivalStatus(ArchivalStatus.ARCHIVAL_STATUS_DISABLED)
          .setVisibilityArchivalUri("visibilityArchivalUri")
          .addAllClusters(ImmutableList.of(CLUSTER_REPLICATION_CONFIGURATION))
          .setActiveClusterName("activeCluster")
          .setDeleteBadBinary("deleteBadBinary")
          .setFailoverTimeout(seconds(1))
          .setUpdateMask(
              FieldMask.newBuilder()
                  .addPaths("description")
                  .addPaths("owner_email")
                  .addPaths("data")
                  .addPaths("workflow_execution_retention_period")
                  .addPaths("bad_binaries")
                  .addPaths("history_archival_status")
                  .addPaths("history_archival_uri")
                  .addPaths("visibility_archival_status")
                  .addPaths("visibility_archival_uri")
                  .addPaths("active_cluster_name")
                  .addPaths("clusters")
                  .addPaths("delete_bad_binary")
                  .addPaths("failover_timeout")
                  .build())
          .build();

  public static final ListClosedWorkflowExecutionsRequest LIST_CLOSED_WORKFLOW_EXECUTIONS_REQUEST =
      ListClosedWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setExecutionFilter(WORKFLOW_EXECUTION_FILTER)
          .setTypeFilter(WORKFLOW_TYPE_FILTER)
          .setStatusFilter(STATUS_FILTER)
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(START_TIME_FILTER)
          .build();

  public static final ListOpenWorkflowExecutionsRequest LIST_OPEN_WORKFLOW_EXECUTIONS_REQUEST =
      ListOpenWorkflowExecutionsRequest.newBuilder()
          .setDomain("domain")
          .setPageSize(1)
          .setExecutionFilter(WORKFLOW_EXECUTION_FILTER)
          .setTypeFilter(WORKFLOW_TYPE_FILTER)
          .setNextPageToken(utf8("nextPageToken"))
          .setStartTimeFilter(START_TIME_FILTER)
          .build();

  public static final StartWorkflowExecutionResponse START_WORKFLOW_EXECUTION_RESPONSE =
      StartWorkflowExecutionResponse.newBuilder().setRunId(WORKFLOW_EXECUTION.getRunId()).build();
  public static final StartWorkflowExecutionAsyncResponse START_WORKFLOW_EXECUTION_ASYNC_RESPONSE =
      StartWorkflowExecutionAsyncResponse.newBuilder().build();

  public static final DescribeTaskListResponse DESCRIBE_TASK_LIST_RESPONSE =
      DescribeTaskListResponse.newBuilder()
          .addPollers(POLLER_INFO)
          .setTaskListStatus(TASK_LIST_STATUS)
          .build();

  public static final DescribeWorkflowExecutionResponse DESCRIBE_WORKFLOW_EXECUTION_RESPONSE =
      DescribeWorkflowExecutionResponse.newBuilder()
          .setExecutionConfiguration(WORKFLOW_EXECUTION_CONFIGURATION)
          .setWorkflowExecutionInfo(WORKFLOW_EXECUTION_INFO)
          .addPendingActivities(PENDING_ACTIVITY_INFO)
          .addPendingChildren(PENDING_CHILD_EXECUTION_INFO)
          .setPendingDecision(PENDING_DECISION_INFO)
          .build();

  public static final GetClusterInfoResponse GET_CLUSTER_INFO_RESPONSE =
      GetClusterInfoResponse.newBuilder()
          .setSupportedClientVersions(SUPPORTED_CLIENT_VERSIONS)
          .build();

  public static final GetSearchAttributesResponse GET_SEARCH_ATTRIBUTES_RESPONSE =
      GetSearchAttributesResponse.newBuilder().putAllKeys(INDEXED_VALUES).build();
  public static final GetWorkflowExecutionHistoryResponse GET_WORKFLOW_EXECUTION_HISTORY_RESPONSE =
      GetWorkflowExecutionHistoryResponse.newBuilder()
          .setHistory(HISTORY)
          .addRawHistory(DATA_BLOB)
          .setNextPageToken(utf8("nextPageToken"))
          .setArchived(true)
          .build();

  public static final ListArchivedWorkflowExecutionsResponse
      LIST_ARCHIVED_WORKFLOW_EXECUTIONS_RESPONSE =
          ListArchivedWorkflowExecutionsResponse.newBuilder()
              .addExecutions(WORKFLOW_EXECUTION_INFO)
              .setNextPageToken(utf8("nextPageToken"))
              .build();

  public static final ListClosedWorkflowExecutionsResponse
      LIST_CLOSED_WORKFLOW_EXECUTIONS_RESPONSE =
          ListClosedWorkflowExecutionsResponse.newBuilder()
              .addExecutions(WORKFLOW_EXECUTION_INFO)
              .setNextPageToken(utf8("nextPageToken"))
              .build();
  public static final ListOpenWorkflowExecutionsResponse LIST_OPEN_WORKFLOW_EXECUTIONS_RESPONSE =
      ListOpenWorkflowExecutionsResponse.newBuilder()
          .addExecutions(WORKFLOW_EXECUTION_INFO)
          .setNextPageToken(utf8("nextPageToken"))
          .build();
  public static final ListTaskListPartitionsResponse LIST_TASK_LIST_PARTITIONS_RESPONSE =
      ListTaskListPartitionsResponse.newBuilder()
          .addActivityTaskListPartitions(TASK_LIST_PARTITION_METADATA)
          .addDecisionTaskListPartitions(TASK_LIST_PARTITION_METADATA)
          .build();
  public static final ScanWorkflowExecutionsResponse SCAN_WORKFLOW_EXECUTIONS_RESPONSE =
      ScanWorkflowExecutionsResponse.newBuilder()
          .addExecutions(WORKFLOW_EXECUTION_INFO)
          .setNextPageToken(utf8("nextPageToken"))
          .build();
  public static final ListWorkflowExecutionsResponse LIST_WORKFLOW_EXECUTIONS_RESPONSE =
      ListWorkflowExecutionsResponse.newBuilder()
          .addExecutions(WORKFLOW_EXECUTION_INFO)
          .setNextPageToken(utf8("nextPageToken"))
          .build();
  public static final PollForActivityTaskResponse POLL_FOR_ACTIVITY_TASK_RESPONSE =
      PollForActivityTaskResponse.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setActivityId("activityId")
          .setActivityType(ACTIVITY_TYPE)
          .setInput(payload("input"))
          .setScheduledTime(timestampNanos(1))
          .setStartedTime(timestampNanos(2))
          .setScheduleToCloseTimeout(seconds(3))
          .setStartToCloseTimeout(seconds(4))
          .setHeartbeatTimeout(seconds(5))
          .setAttempt(6)
          .setScheduledTimeOfThisAttempt(timestampNanos(7))
          .setHeartbeatDetails(payload("heartbeatDetails"))
          .setWorkflowType(WORKFLOW_TYPE)
          .setWorkflowDomain("domain")
          .setHeader(HEADER)
          .setAutoConfigHint(AUTO_CONFIG_HINT)
          .build();
  public static final PollForDecisionTaskResponse POLL_FOR_DECISION_TASK_RESPONSE =
      PollForDecisionTaskResponse.newBuilder()
          .setTaskToken(utf8("taskToken"))
          .setWorkflowExecution(WORKFLOW_EXECUTION)
          .setWorkflowType(WORKFLOW_TYPE)
          .setPreviousStartedEventId(int64(1))
          .setStartedEventId(2)
          .setAttempt(3)
          .setBacklogCountHint(4)
          .setHistory(HISTORY)
          .setNextPageToken(utf8("nextPageToken"))
          .setQuery(WORKFLOW_QUERY)
          .setWorkflowExecutionTaskList(TASK_LIST)
          .setScheduledTime(timestampNanos(5))
          .setStartedTime(timestampNanos(6))
          .putAllQueries(ImmutableMap.of("query", WORKFLOW_QUERY))
          .setNextEventId(7)
          .setAutoConfigHint(AUTO_CONFIG_HINT)
          .build();

  public static final QueryWorkflowResponse QUERY_WORKFLOW_RESPONSE =
      QueryWorkflowResponse.newBuilder()
          .setQueryResult(payload("result"))
          .setQueryRejected(
              QueryRejected.newBuilder()
                  .setCloseStatus(
                      WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED))
          .build();

  public static final RecordActivityTaskHeartbeatResponse RECORD_ACTIVITY_TASK_HEARTBEAT_RESPONSE =
      RecordActivityTaskHeartbeatResponse.newBuilder().setCancelRequested(true).build();
  public static final RecordActivityTaskHeartbeatByIDResponse
      RECORD_ACTIVITY_TASK_HEARTBEAT_BY_ID_RESPONSE =
          RecordActivityTaskHeartbeatByIDResponse.newBuilder().setCancelRequested(true).build();
  public static final ResetWorkflowExecutionResponse RESET_WORKFLOW_EXECUTION_RESPONSE =
      ResetWorkflowExecutionResponse.newBuilder().setRunId(WORKFLOW_EXECUTION.getRunId()).build();
  public static final RespondDecisionTaskCompletedResponse
      RESPOND_DECISION_TASK_COMPLETED_RESPONSE =
          RespondDecisionTaskCompletedResponse.newBuilder()
              .setDecisionTask(POLL_FOR_DECISION_TASK_RESPONSE)
              .putActivitiesToDispatchLocally("activity", ACTIVITY_LOCAL_DISPATCH_INFO)
              .build();
  public static final CountWorkflowExecutionsResponse COUNT_WORKFLOW_EXECUTIONS_RESPONSE =
      CountWorkflowExecutionsResponse.newBuilder().setCount(1000).build();
  public static final DescribeDomainResponse DESCRIBE_DOMAIN_RESPONSE =
      DescribeDomainResponse.newBuilder().setDomain(DOMAIN).build();
  public static final ListDomainsResponse LIST_DOMAINS_RESPONSE =
      ListDomainsResponse.newBuilder()
          .addDomains(DOMAIN)
          .setNextPageToken(utf8("nextPageToken"))
          .build();
  public static final SignalWithStartWorkflowExecutionResponse
      SIGNAL_WITH_START_WORKFLOW_EXECUTION_RESPONSE =
          SignalWithStartWorkflowExecutionResponse.newBuilder()
              .setRunId(WORKFLOW_EXECUTION.getRunId())
              .build();
  public static final SignalWithStartWorkflowExecutionAsyncResponse
      SIGNAL_WITH_START_WORKFLOW_EXECUTION_ASYNC_RESPONSE =
          SignalWithStartWorkflowExecutionAsyncResponse.newBuilder().build();
  public static final UpdateDomainResponse UPDATE_DOMAIN_RESPONSE =
      UpdateDomainResponse.newBuilder().setDomain(DOMAIN).build();

  public static final GetSearchAttributesRequest GET_SEARCH_ATTRIBUTES_REQUEST =
      GetSearchAttributesRequest.getDefaultInstance();

  public static final RegisterDomainResponse REGISTER_DOMAIN_RESPONSE =
      RegisterDomainResponse.getDefaultInstance();

  public static final DeprecateDomainResponse DEPRECATE_DOMAIN_RESPONSE =
      DeprecateDomainResponse.getDefaultInstance();

  public static final SignalWorkflowExecutionResponse SIGNAL_WORKFLOW_EXECUTION_RESPONSE =
      SignalWorkflowExecutionResponse.getDefaultInstance();

  public static final RequestCancelWorkflowExecutionResponse
      REQUEST_CANCEL_WORKFLOW_EXECUTION_RESPONSE =
          RequestCancelWorkflowExecutionResponse.getDefaultInstance();

  public static final TerminateWorkflowExecutionResponse TERMINATE_WORKFLOW_EXECUTION_RESPONSE =
      TerminateWorkflowExecutionResponse.getDefaultInstance();

  public static final GetClusterInfoRequest GET_CLUSTER_INFO_REQUEST =
      GetClusterInfoRequest.getDefaultInstance();

  public static final RespondDecisionTaskFailedResponse RESPOND_DECISION_TASK_FAILED_RESPONSE =
      RespondDecisionTaskFailedResponse.getDefaultInstance();

  public static final RespondActivityTaskCompletedResponse
      RESPOND_ACTIVITY_TASK_COMPLETED_RESPONSE =
          RespondActivityTaskCompletedResponse.getDefaultInstance();

  public static final RespondActivityTaskCompletedByIDResponse
      RESPOND_ACTIVITY_TASK_COMPLETED_BY_ID_RESPONSE =
          RespondActivityTaskCompletedByIDResponse.getDefaultInstance();

  public static final RespondActivityTaskFailedResponse RESPOND_ACTIVITY_TASK_FAILED_RESPONSE =
      RespondActivityTaskFailedResponse.getDefaultInstance();

  public static final RespondActivityTaskFailedByIDResponse
      RESPOND_ACTIVITY_TASK_FAILED_BY_ID_RESPONSE =
          RespondActivityTaskFailedByIDResponse.getDefaultInstance();

  public static final RespondActivityTaskCanceledResponse RESPOND_ACTIVITY_TASK_CANCELED_RESPONSE =
      RespondActivityTaskCanceledResponse.getDefaultInstance();

  public static final RespondActivityTaskCanceledByIDResponse
      RESPOND_ACTIVITY_TASK_CANCELED_BY_ID_RESPONSE =
          RespondActivityTaskCanceledByIDResponse.getDefaultInstance();

  public static final RespondQueryTaskCompletedResponse RESPOND_QUERY_TASK_COMPLETED_RESPONSE =
      RespondQueryTaskCompletedResponse.getDefaultInstance();

  public static final ResetStickyTaskListResponse RESET_STICKY_TASK_LIST_RESPONSE =
      ResetStickyTaskListResponse.getDefaultInstance();

  public static final RefreshWorkflowTasksRequest REFRESH_WORKFLOW_TASKS_REQUEST =
      RefreshWorkflowTasksRequest.getDefaultInstance();

  public static final RefreshWorkflowTasksResponse REFRESH_WORKFLOW_TASKS_RESPONSE =
      RefreshWorkflowTasksResponse.getDefaultInstance();

  private ProtoObjects() {}

  public static Payload payload(String value) {
    return Payload.newBuilder().setData(utf8(value)).build();
  }

  private static Duration seconds(int value) {
    return Duration.newBuilder().setSeconds(value).build();
  }

  private static Duration days(int value) {
    return Duration.newBuilder().setSeconds(((long) value) * 24 * 60 * 60).build();
  }

  private static Timestamp timestampNanos(int value) {
    return Timestamp.newBuilder().setNanos(value).build();
  }

  private static ByteString utf8(String value) {
    return ByteString.copyFromUtf8(value);
  }

  private static Int64Value int64(long value) {
    return Int64Value.newBuilder().setValue(value).build();
  }
}
