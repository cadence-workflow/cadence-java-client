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

import com.uber.cadence.api.v1.ArchivalStatus;
import com.uber.cadence.api.v1.ContinueAsNewInitiator;
import com.uber.cadence.api.v1.DecisionTaskFailedCause;
import com.uber.cadence.api.v1.EventFilterType;
import com.uber.cadence.api.v1.ParentClosePolicy;
import com.uber.cadence.api.v1.QueryConsistencyLevel;
import com.uber.cadence.api.v1.QueryRejectCondition;
import com.uber.cadence.api.v1.QueryResultType;
import com.uber.cadence.api.v1.TaskListKind;
import com.uber.cadence.api.v1.TaskListType;
import com.uber.cadence.api.v1.WorkflowExecutionCloseStatus;
import com.uber.cadence.api.v1.WorkflowIdReusePolicy;

public final class EnumMapper {

  private EnumMapper() {}

  public static TaskListKind taskListKind(com.uber.cadence.TaskListKind t) {
    if (t == null) {
      return TaskListKind.TASK_LIST_KIND_INVALID;
    }
    switch (t) {
      case NORMAL:
        return TaskListKind.TASK_LIST_KIND_NORMAL;
      case STICKY:
        return TaskListKind.TASK_LIST_KIND_STICKY;
      case EPHEMERAL:
        return TaskListKind.TASK_LIST_KIND_EPHEMERAL;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static TaskListType taskListType(com.uber.cadence.TaskListType t) {
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

  public static EventFilterType eventFilterType(com.uber.cadence.HistoryEventFilterType t) {
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

  public static QueryRejectCondition queryRejectCondition(com.uber.cadence.QueryRejectCondition t) {
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
      com.uber.cadence.QueryConsistencyLevel t) {
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
      com.uber.cadence.ContinueAsNewInitiator t) {
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
      com.uber.cadence.WorkflowIdReusePolicy t) {
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

  public static QueryResultType queryResultType(com.uber.cadence.QueryResultType t) {
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

  public static ArchivalStatus archivalStatus(com.uber.cadence.ArchivalStatus t) {
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

  public static ParentClosePolicy parentClosePolicy(com.uber.cadence.ParentClosePolicy t) {
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
      com.uber.cadence.DecisionTaskFailedCause t) {
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
      com.uber.cadence.WorkflowExecutionCloseStatus t) {
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

  public static QueryResultType queryTaskCompletedType(com.uber.cadence.QueryTaskCompletedType t) {
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
}
