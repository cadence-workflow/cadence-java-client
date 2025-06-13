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

import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_BINARY;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_INVALID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION;
import static com.uber.cadence.api.v1.DecisionTaskFailedCause.DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_ANSWERED;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_FAILED;
import static com.uber.cadence.api.v1.QueryResultType.QUERY_RESULT_TYPE_INVALID;

import com.uber.cadence.api.v1.*;

public final class EnumMapper {

  private EnumMapper() {}

  public static TaskListKind taskListKind(com.uber.cadence.entities.TaskListKind t) {
    if (t == null) {
      return TaskListKind.TASK_LIST_KIND_INVALID;
    }
    switch (t) {
      case NORMAL:
        return TaskListKind.TASK_LIST_KIND_NORMAL;
      case STICKY:
        return TaskListKind.TASK_LIST_KIND_STICKY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static TaskListType taskListType(com.uber.cadence.entities.TaskListType t) {
    if (t == null) {
      return TaskListType.TASK_LIST_TYPE_INVALID;
    }
    switch (t) {
      case Decision:
        return TaskListType.TASK_LIST_TYPE_DECISION;
      case Activity:
        return TaskListType.TASK_LIST_TYPE_ACTIVITY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static EventFilterType eventFilterType(
      com.uber.cadence.entities.HistoryEventFilterType t) {
    if (t == null) {
      return EventFilterType.EVENT_FILTER_TYPE_INVALID;
    }
    switch (t) {
      case ALL_EVENT:
        return EventFilterType.EVENT_FILTER_TYPE_ALL_EVENT;
      case CLOSE_EVENT:
        return EventFilterType.EVENT_FILTER_TYPE_CLOSE_EVENT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static QueryRejectCondition queryRejectCondition(
      com.uber.cadence.entities.QueryRejectCondition t) {
    if (t == null) {
      return QueryRejectCondition.QUERY_REJECT_CONDITION_INVALID;
    }
    switch (t) {
      case NOT_OPEN:
        return QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_OPEN;
      case NOT_COMPLETED_CLEANLY:
        return QueryRejectCondition.QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static QueryConsistencyLevel queryConsistencyLevel(
      com.uber.cadence.entities.QueryConsistencyLevel t) {
    if (t == null) {
      return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_INVALID;
    }
    switch (t) {
      case EVENTUAL:
        return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_EVENTUAL;
      case STRONG:
        return QueryConsistencyLevel.QUERY_CONSISTENCY_LEVEL_STRONG;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ContinueAsNewInitiator continueAsNewInitiator(
      com.uber.cadence.entities.ContinueAsNewInitiator t) {
    if (t == null) {
      return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_INVALID;
    }
    switch (t) {
      case Decider:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_DECIDER;
      case RetryPolicy:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY;
      case CronSchedule:
        return ContinueAsNewInitiator.CONTINUE_AS_NEW_INITIATOR_CRON_SCHEDULE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static WorkflowIdReusePolicy workflowIdReusePolicy(
      com.uber.cadence.entities.WorkflowIdReusePolicy t) {
    if (t == null) {
      return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_INVALID;
    }
    switch (t) {
      case AllowDuplicateFailedOnly:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY;
      case AllowDuplicate:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE;
      case RejectDuplicate:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE;
      case TerminateIfRunning:
        return WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static QueryResultType queryResultType(com.uber.cadence.entities.QueryResultType t) {
    if (t == null) {
      return QUERY_RESULT_TYPE_INVALID;
    }
    switch (t) {
      case ANSWERED:
        return QUERY_RESULT_TYPE_ANSWERED;
      case FAILED:
        return QUERY_RESULT_TYPE_FAILED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ArchivalStatus archivalStatus(com.uber.cadence.entities.ArchivalStatus t) {
    if (t == null) {
      return ArchivalStatus.ARCHIVAL_STATUS_INVALID;
    }
    switch (t) {
      case DISABLED:
        return ArchivalStatus.ARCHIVAL_STATUS_DISABLED;
      case ENABLED:
        return ArchivalStatus.ARCHIVAL_STATUS_ENABLED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ParentClosePolicy parentClosePolicy(com.uber.cadence.entities.ParentClosePolicy t) {
    if (t == null) {
      return ParentClosePolicy.PARENT_CLOSE_POLICY_INVALID;
    }
    switch (t) {
      case ABANDON:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON;
      case REQUEST_CANCEL:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL;
      case TERMINATE:
        return ParentClosePolicy.PARENT_CLOSE_POLICY_TERMINATE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static DecisionTaskFailedCause decisionTaskFailedCause(
      com.uber.cadence.entities.DecisionTaskFailedCause t) {
    if (t == null) {
      return DECISION_TASK_FAILED_CAUSE_INVALID;
    }
    switch (t) {
      case UNHANDLED_DECISION:
        return DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION;
      case BAD_SCHEDULE_ACTIVITY_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
      case BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
      case BAD_START_TIMER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES;
      case BAD_CANCEL_TIMER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES;
      case BAD_RECORD_MARKER_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES;
      case BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_CONTINUE_AS_NEW_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES;
      case START_TIMER_DUPLICATE_ID:
        return DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID;
      case RESET_STICKY_TASKLIST:
        return DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST;
      case WORKFLOW_WORKER_UNHANDLED_FAILURE:
        return DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE;
      case BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case BAD_START_CHILD_EXECUTION_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES;
      case FORCE_CLOSE_DECISION:
        return DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION;
      case FAILOVER_CLOSE_DECISION:
        return DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION;
      case BAD_SIGNAL_INPUT_SIZE:
        return DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE;
      case RESET_WORKFLOW:
        return DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW;
      case BAD_BINARY:
        return DECISION_TASK_FAILED_CAUSE_BAD_BINARY;
      case SCHEDULE_ACTIVITY_DUPLICATE_ID:
        return DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID;
      case BAD_SEARCH_ATTRIBUTES:
        return DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static WorkflowExecutionCloseStatus workflowExecutionCloseStatus(
      com.uber.cadence.entities.WorkflowExecutionCloseStatus t) {
    if (t == null) {
      return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_INVALID;
    }
    switch (t) {
      case COMPLETED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED;
      case FAILED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED;
      case CANCELED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_CANCELED;
      case TERMINATED:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_TERMINATED;
      case CONTINUED_AS_NEW:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_CONTINUED_AS_NEW;
      case TIMED_OUT:
        return WorkflowExecutionCloseStatus.WORKFLOW_EXECUTION_CLOSE_STATUS_TIMED_OUT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static QueryResultType queryTaskCompletedType(
      com.uber.cadence.entities.QueryTaskCompletedType t) {
    if (t == null) {
      return QUERY_RESULT_TYPE_INVALID;
    }
    switch (t) {
      case COMPLETED:
        return QUERY_RESULT_TYPE_ANSWERED;
      case FAILED:
        return QUERY_RESULT_TYPE_FAILED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.TaskListKind taskListKind(TaskListKind t) {
    switch (t) {
      case TASK_LIST_KIND_INVALID:
        return null;
      case TASK_LIST_KIND_NORMAL:
        return com.uber.cadence.entities.TaskListKind.NORMAL;
      case TASK_LIST_KIND_STICKY:
        return com.uber.cadence.entities.TaskListKind.STICKY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.QueryRejectCondition queryRejectCondition(
      QueryRejectCondition t) {
    if (t == QueryRejectCondition.QUERY_REJECT_CONDITION_INVALID) {
      return null;
    }
    switch (t) {
      case QUERY_REJECT_CONDITION_NOT_OPEN:
        return com.uber.cadence.entities.QueryRejectCondition.NOT_OPEN;
      case QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY:
        return com.uber.cadence.entities.QueryRejectCondition.NOT_COMPLETED_CLEANLY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.ContinueAsNewInitiator continueAsNewInitiator(
      ContinueAsNewInitiator t) {
    switch (t) {
      case CONTINUE_AS_NEW_INITIATOR_INVALID:
        return null;
      case CONTINUE_AS_NEW_INITIATOR_DECIDER:
        return com.uber.cadence.entities.ContinueAsNewInitiator.Decider;
      case CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY:
        return com.uber.cadence.entities.ContinueAsNewInitiator.RetryPolicy;
      case CONTINUE_AS_NEW_INITIATOR_CRON_SCHEDULE:
        return com.uber.cadence.entities.ContinueAsNewInitiator.CronSchedule;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.WorkflowIdReusePolicy workflowIdReusePolicy(
      WorkflowIdReusePolicy t) {
    switch (t) {
      case WORKFLOW_ID_REUSE_POLICY_INVALID:
        return null;
      case WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY:
        return com.uber.cadence.entities.WorkflowIdReusePolicy.AllowDuplicateFailedOnly;
      case WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE:
        return com.uber.cadence.entities.WorkflowIdReusePolicy.AllowDuplicate;
      case WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE:
        return com.uber.cadence.entities.WorkflowIdReusePolicy.RejectDuplicate;
      case WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING:
        return com.uber.cadence.entities.WorkflowIdReusePolicy.TerminateIfRunning;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.ArchivalStatus archivalStatus(ArchivalStatus t) {
    switch (t) {
      case ARCHIVAL_STATUS_INVALID:
        return null;
      case ARCHIVAL_STATUS_DISABLED:
        return com.uber.cadence.entities.ArchivalStatus.DISABLED;
      case ARCHIVAL_STATUS_ENABLED:
        return com.uber.cadence.entities.ArchivalStatus.ENABLED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.ParentClosePolicy parentClosePolicy(ParentClosePolicy t) {
    switch (t) {
      case PARENT_CLOSE_POLICY_INVALID:
        return null;
      case PARENT_CLOSE_POLICY_ABANDON:
        return com.uber.cadence.entities.ParentClosePolicy.ABANDON;
      case PARENT_CLOSE_POLICY_REQUEST_CANCEL:
        return com.uber.cadence.entities.ParentClosePolicy.REQUEST_CANCEL;
      case PARENT_CLOSE_POLICY_TERMINATE:
        return com.uber.cadence.entities.ParentClosePolicy.TERMINATE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.DecisionTaskFailedCause decisionTaskFailedCause(
      DecisionTaskFailedCause t) {
    switch (t) {
      case DECISION_TASK_FAILED_CAUSE_INVALID:
        return null;
      case DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION:
        return com.uber.cadence.entities.DecisionTaskFailedCause.UNHANDLED_DECISION;
      case DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_START_TIMER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_CANCEL_TIMER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_RECORD_MARKER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_CONTINUE_AS_NEW_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID:
        return com.uber.cadence.entities.DecisionTaskFailedCause.START_TIMER_DUPLICATE_ID;
      case DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST:
        return com.uber.cadence.entities.DecisionTaskFailedCause.RESET_STICKY_TASKLIST;
      case DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE:
        return com.uber.cadence.entities.DecisionTaskFailedCause.WORKFLOW_WORKER_UNHANDLED_FAILURE;
      case DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause
            .BAD_START_CHILD_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION:
        return com.uber.cadence.entities.DecisionTaskFailedCause.FORCE_CLOSE_DECISION;
      case DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION:
        return com.uber.cadence.entities.DecisionTaskFailedCause.FAILOVER_CLOSE_DECISION;
      case DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_SIGNAL_INPUT_SIZE;
      case DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW:
        return com.uber.cadence.entities.DecisionTaskFailedCause.RESET_WORKFLOW;
      case DECISION_TASK_FAILED_CAUSE_BAD_BINARY:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_BINARY;
      case DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID:
        return com.uber.cadence.entities.DecisionTaskFailedCause.SCHEDULE_ACTIVITY_DUPLICATE_ID;
      case DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES:
        return com.uber.cadence.entities.DecisionTaskFailedCause.BAD_SEARCH_ATTRIBUTES;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.WorkflowExecutionCloseStatus workflowExecutionCloseStatus(
      WorkflowExecutionCloseStatus t) {
    switch (t) {
      case WORKFLOW_EXECUTION_CLOSE_STATUS_INVALID:
        return null;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.COMPLETED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.FAILED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_CANCELED:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.CANCELED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_TERMINATED:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.TERMINATED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_CONTINUED_AS_NEW:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.CONTINUED_AS_NEW;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_TIMED_OUT:
        return com.uber.cadence.entities.WorkflowExecutionCloseStatus.TIMED_OUT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.DomainStatus domainStatus(DomainStatus t) {
    switch (t) {
      case DOMAIN_STATUS_INVALID:
        return null;
      case DOMAIN_STATUS_REGISTERED:
        return com.uber.cadence.entities.DomainStatus.REGISTERED;
      case DOMAIN_STATUS_DEPRECATED:
        return com.uber.cadence.entities.DomainStatus.DEPRECATED;
      case DOMAIN_STATUS_DELETED:
        return com.uber.cadence.entities.DomainStatus.DELETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.PendingActivityState pendingActivityState(
      PendingActivityState t) {
    switch (t) {
      case PENDING_ACTIVITY_STATE_INVALID:
        return null;
      case PENDING_ACTIVITY_STATE_SCHEDULED:
        return com.uber.cadence.entities.PendingActivityState.SCHEDULED;
      case PENDING_ACTIVITY_STATE_STARTED:
        return com.uber.cadence.entities.PendingActivityState.STARTED;
      case PENDING_ACTIVITY_STATE_CANCEL_REQUESTED:
        return com.uber.cadence.entities.PendingActivityState.CANCEL_REQUESTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.PendingDecisionState pendingDecisionState(
      PendingDecisionState t) {
    switch (t) {
      case PENDING_DECISION_STATE_INVALID:
        return null;
      case PENDING_DECISION_STATE_SCHEDULED:
        return com.uber.cadence.entities.PendingDecisionState.SCHEDULED;
      case PENDING_DECISION_STATE_STARTED:
        return com.uber.cadence.entities.PendingDecisionState.STARTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.IndexedValueType indexedValueType(IndexedValueType t) {
    switch (t) {
      case INDEXED_VALUE_TYPE_INVALID:
        throw new IllegalArgumentException("received IndexedValueType_INDEXED_VALUE_TYPE_INVALID");
      case INDEXED_VALUE_TYPE_STRING:
        return com.uber.cadence.entities.IndexedValueType.STRING;
      case INDEXED_VALUE_TYPE_KEYWORD:
        return com.uber.cadence.entities.IndexedValueType.KEYWORD;
      case INDEXED_VALUE_TYPE_INT:
        return com.uber.cadence.entities.IndexedValueType.INT;
      case INDEXED_VALUE_TYPE_DOUBLE:
        return com.uber.cadence.entities.IndexedValueType.DOUBLE;
      case INDEXED_VALUE_TYPE_BOOL:
        return com.uber.cadence.entities.IndexedValueType.BOOL;
      case INDEXED_VALUE_TYPE_DATETIME:
        return com.uber.cadence.entities.IndexedValueType.DATETIME;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.EncodingType encodingType(EncodingType t) {
    switch (t) {
      case ENCODING_TYPE_INVALID:
        return null;
      case ENCODING_TYPE_THRIFTRW:
        return com.uber.cadence.entities.EncodingType.ThriftRW;
      case ENCODING_TYPE_JSON:
        return com.uber.cadence.entities.EncodingType.JSON;
      case ENCODING_TYPE_PROTO3:
        throw new UnsupportedOperationException();
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.TimeoutType timeoutType(TimeoutType t) {
    switch (t) {
      case TIMEOUT_TYPE_INVALID:
        return null;
      case TIMEOUT_TYPE_START_TO_CLOSE:
        return com.uber.cadence.entities.TimeoutType.START_TO_CLOSE;
      case TIMEOUT_TYPE_SCHEDULE_TO_START:
        return com.uber.cadence.entities.TimeoutType.SCHEDULE_TO_START;
      case TIMEOUT_TYPE_SCHEDULE_TO_CLOSE:
        return com.uber.cadence.entities.TimeoutType.SCHEDULE_TO_CLOSE;
      case TIMEOUT_TYPE_HEARTBEAT:
        return com.uber.cadence.entities.TimeoutType.HEARTBEAT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.DecisionTaskTimedOutCause decisionTaskTimedOutCause(
      DecisionTaskTimedOutCause t) {
    switch (t) {
      case DECISION_TASK_TIMED_OUT_CAUSE_INVALID:
        return null;
      case DECISION_TASK_TIMED_OUT_CAUSE_TIMEOUT:
        return com.uber.cadence.entities.DecisionTaskTimedOutCause.TIMEOUT;
      case DECISION_TASK_TIMED_OUT_CAUSE_RESET:
        return com.uber.cadence.entities.DecisionTaskTimedOutCause.RESET;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.CancelExternalWorkflowExecutionFailedCause
      cancelExternalWorkflowExecutionFailedCause(CancelExternalWorkflowExecutionFailedCause t) {
    switch (t) {
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return com.uber.cadence.entities.CancelExternalWorkflowExecutionFailedCause
            .UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED:
        return com.uber.cadence.entities.CancelExternalWorkflowExecutionFailedCause
            .WORKFLOW_ALREADY_COMPLETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.SignalExternalWorkflowExecutionFailedCause
      signalExternalWorkflowExecutionFailedCause(SignalExternalWorkflowExecutionFailedCause t) {
    switch (t) {
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return com.uber.cadence.entities.SignalExternalWorkflowExecutionFailedCause
            .UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED:
        return com.uber.cadence.entities.SignalExternalWorkflowExecutionFailedCause
            .WORKFLOW_ALREADY_COMPLETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static com.uber.cadence.entities.ChildWorkflowExecutionFailedCause
      childWorkflowExecutionFailedCause(ChildWorkflowExecutionFailedCause t) {
    switch (t) {
      case CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_RUNNING:
        return com.uber.cadence.entities.ChildWorkflowExecutionFailedCause.WORKFLOW_ALREADY_RUNNING;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }
}
