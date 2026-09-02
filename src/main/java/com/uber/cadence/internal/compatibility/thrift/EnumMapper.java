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

import static com.uber.cadence.api.v1.QueryRejectCondition.QUERY_REJECT_CONDITION_INVALID;

import com.uber.cadence.ArchivalStatus;
import com.uber.cadence.CancelExternalWorkflowExecutionFailedCause;
import com.uber.cadence.ChildWorkflowExecutionFailedCause;
import com.uber.cadence.ContinueAsNewInitiator;
import com.uber.cadence.DecisionTaskFailedCause;
import com.uber.cadence.DecisionTaskTimedOutCause;
import com.uber.cadence.DomainStatus;
import com.uber.cadence.EncodingType;
import com.uber.cadence.IndexedValueType;
import com.uber.cadence.ParentClosePolicy;
import com.uber.cadence.PendingActivityState;
import com.uber.cadence.PendingDecisionState;
import com.uber.cadence.QueryRejectCondition;
import com.uber.cadence.SignalExternalWorkflowExecutionFailedCause;
import com.uber.cadence.TaskListKind;
import com.uber.cadence.TimeoutType;
import com.uber.cadence.WorkflowExecutionCloseStatus;
import com.uber.cadence.WorkflowIdReusePolicy;

public final class EnumMapper {

  private EnumMapper() {}

  public static TaskListKind taskListKind(com.uber.cadence.api.v1.TaskListKind t) {
    switch (t) {
      case TASK_LIST_KIND_INVALID:
        return null;
      case TASK_LIST_KIND_NORMAL:
        return TaskListKind.NORMAL;
      case TASK_LIST_KIND_STICKY:
        return TaskListKind.STICKY;
      case TASK_LIST_KIND_EPHEMERAL:
        return TaskListKind.EPHEMERAL;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static QueryRejectCondition queryRejectCondition(
      com.uber.cadence.api.v1.QueryRejectCondition t) {
    if (t == QUERY_REJECT_CONDITION_INVALID) {
      return null;
    }
    switch (t) {
      case QUERY_REJECT_CONDITION_NOT_OPEN:
        return QueryRejectCondition.NOT_OPEN;
      case QUERY_REJECT_CONDITION_NOT_COMPLETED_CLEANLY:
        return QueryRejectCondition.NOT_COMPLETED_CLEANLY;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ContinueAsNewInitiator continueAsNewInitiator(
      com.uber.cadence.api.v1.ContinueAsNewInitiator t) {
    switch (t) {
      case CONTINUE_AS_NEW_INITIATOR_INVALID:
        return null;
      case CONTINUE_AS_NEW_INITIATOR_DECIDER:
        return ContinueAsNewInitiator.Decider;
      case CONTINUE_AS_NEW_INITIATOR_RETRY_POLICY:
        return ContinueAsNewInitiator.RetryPolicy;
      case CONTINUE_AS_NEW_INITIATOR_CRON_SCHEDULE:
        return ContinueAsNewInitiator.CronSchedule;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static WorkflowIdReusePolicy workflowIdReusePolicy(
      com.uber.cadence.api.v1.WorkflowIdReusePolicy t) {
    switch (t) {
      case WORKFLOW_ID_REUSE_POLICY_INVALID:
        return null;
      case WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY:
        return WorkflowIdReusePolicy.AllowDuplicateFailedOnly;
      case WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE:
        return WorkflowIdReusePolicy.AllowDuplicate;
      case WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE:
        return WorkflowIdReusePolicy.RejectDuplicate;
      case WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING:
        return WorkflowIdReusePolicy.TerminateIfRunning;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ArchivalStatus archivalStatus(com.uber.cadence.api.v1.ArchivalStatus t) {
    switch (t) {
      case ARCHIVAL_STATUS_INVALID:
        return null;
      case ARCHIVAL_STATUS_DISABLED:
        return ArchivalStatus.DISABLED;
      case ARCHIVAL_STATUS_ENABLED:
        return ArchivalStatus.ENABLED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ParentClosePolicy parentClosePolicy(com.uber.cadence.api.v1.ParentClosePolicy t) {
    switch (t) {
      case PARENT_CLOSE_POLICY_INVALID:
        return null;
      case PARENT_CLOSE_POLICY_ABANDON:
        return ParentClosePolicy.ABANDON;
      case PARENT_CLOSE_POLICY_REQUEST_CANCEL:
        return ParentClosePolicy.REQUEST_CANCEL;
      case PARENT_CLOSE_POLICY_TERMINATE:
        return ParentClosePolicy.TERMINATE;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static DecisionTaskFailedCause decisionTaskFailedCause(
      com.uber.cadence.api.v1.DecisionTaskFailedCause t) {
    switch (t) {
      case DECISION_TASK_FAILED_CAUSE_INVALID:
        return null;
      case DECISION_TASK_FAILED_CAUSE_UNHANDLED_DECISION:
        return DecisionTaskFailedCause.UNHANDLED_DECISION;
      case DECISION_TASK_FAILED_CAUSE_BAD_SCHEDULE_ACTIVITY_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_SCHEDULE_ACTIVITY_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_REQUEST_CANCEL_ACTIVITY_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_START_TIMER_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_START_TIMER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_TIMER_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_CANCEL_TIMER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_RECORD_MARKER_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_RECORD_MARKER_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_COMPLETE_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_FAIL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_CANCEL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_REQUEST_CANCEL_EXTERNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_CONTINUE_AS_NEW_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_CONTINUE_AS_NEW_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_START_TIMER_DUPLICATE_ID:
        return DecisionTaskFailedCause.START_TIMER_DUPLICATE_ID;
      case DECISION_TASK_FAILED_CAUSE_RESET_STICKY_TASK_LIST:
        return DecisionTaskFailedCause.RESET_STICKY_TASKLIST;
      case DECISION_TASK_FAILED_CAUSE_WORKFLOW_WORKER_UNHANDLED_FAILURE:
        return DecisionTaskFailedCause.WORKFLOW_WORKER_UNHANDLED_FAILURE;
      case DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_SIGNAL_WORKFLOW_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_BAD_START_CHILD_EXECUTION_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_START_CHILD_EXECUTION_ATTRIBUTES;
      case DECISION_TASK_FAILED_CAUSE_FORCE_CLOSE_DECISION:
        return DecisionTaskFailedCause.FORCE_CLOSE_DECISION;
      case DECISION_TASK_FAILED_CAUSE_FAILOVER_CLOSE_DECISION:
        return DecisionTaskFailedCause.FAILOVER_CLOSE_DECISION;
      case DECISION_TASK_FAILED_CAUSE_BAD_SIGNAL_INPUT_SIZE:
        return DecisionTaskFailedCause.BAD_SIGNAL_INPUT_SIZE;
      case DECISION_TASK_FAILED_CAUSE_RESET_WORKFLOW:
        return DecisionTaskFailedCause.RESET_WORKFLOW;
      case DECISION_TASK_FAILED_CAUSE_BAD_BINARY:
        return DecisionTaskFailedCause.BAD_BINARY;
      case DECISION_TASK_FAILED_CAUSE_SCHEDULE_ACTIVITY_DUPLICATE_ID:
        return DecisionTaskFailedCause.SCHEDULE_ACTIVITY_DUPLICATE_ID;
      case DECISION_TASK_FAILED_CAUSE_BAD_SEARCH_ATTRIBUTES:
        return DecisionTaskFailedCause.BAD_SEARCH_ATTRIBUTES;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static WorkflowExecutionCloseStatus workflowExecutionCloseStatus(
      com.uber.cadence.api.v1.WorkflowExecutionCloseStatus t) {
    switch (t) {
      case WORKFLOW_EXECUTION_CLOSE_STATUS_INVALID:
        return null;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_COMPLETED:
        return WorkflowExecutionCloseStatus.COMPLETED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_FAILED:
        return WorkflowExecutionCloseStatus.FAILED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_CANCELED:
        return WorkflowExecutionCloseStatus.CANCELED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_TERMINATED:
        return WorkflowExecutionCloseStatus.TERMINATED;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_CONTINUED_AS_NEW:
        return WorkflowExecutionCloseStatus.CONTINUED_AS_NEW;
      case WORKFLOW_EXECUTION_CLOSE_STATUS_TIMED_OUT:
        return WorkflowExecutionCloseStatus.TIMED_OUT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static DomainStatus domainStatus(com.uber.cadence.api.v1.DomainStatus t) {
    switch (t) {
      case DOMAIN_STATUS_INVALID:
        return null;
      case DOMAIN_STATUS_REGISTERED:
        return DomainStatus.REGISTERED;
      case DOMAIN_STATUS_DEPRECATED:
        return DomainStatus.DEPRECATED;
      case DOMAIN_STATUS_DELETED:
        return DomainStatus.DELETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static PendingActivityState pendingActivityState(
      com.uber.cadence.api.v1.PendingActivityState t) {
    switch (t) {
      case PENDING_ACTIVITY_STATE_INVALID:
        return null;
      case PENDING_ACTIVITY_STATE_SCHEDULED:
        return PendingActivityState.SCHEDULED;
      case PENDING_ACTIVITY_STATE_STARTED:
        return PendingActivityState.STARTED;
      case PENDING_ACTIVITY_STATE_CANCEL_REQUESTED:
        return PendingActivityState.CANCEL_REQUESTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static PendingDecisionState pendingDecisionState(
      com.uber.cadence.api.v1.PendingDecisionState t) {
    switch (t) {
      case PENDING_DECISION_STATE_INVALID:
        return null;
      case PENDING_DECISION_STATE_SCHEDULED:
        return PendingDecisionState.SCHEDULED;
      case PENDING_DECISION_STATE_STARTED:
        return PendingDecisionState.STARTED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static IndexedValueType indexedValueType(com.uber.cadence.api.v1.IndexedValueType t) {
    switch (t) {
      case INDEXED_VALUE_TYPE_INVALID:
        throw new IllegalArgumentException("received IndexedValueType_INDEXED_VALUE_TYPE_INVALID");
      case INDEXED_VALUE_TYPE_STRING:
        return IndexedValueType.STRING;
      case INDEXED_VALUE_TYPE_KEYWORD:
        return IndexedValueType.KEYWORD;
      case INDEXED_VALUE_TYPE_INT:
        return IndexedValueType.INT;
      case INDEXED_VALUE_TYPE_DOUBLE:
        return IndexedValueType.DOUBLE;
      case INDEXED_VALUE_TYPE_BOOL:
        return IndexedValueType.BOOL;
      case INDEXED_VALUE_TYPE_DATETIME:
        return IndexedValueType.DATETIME;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static EncodingType encodingType(com.uber.cadence.api.v1.EncodingType t) {
    switch (t) {
      case ENCODING_TYPE_INVALID:
        return null;
      case ENCODING_TYPE_THRIFTRW:
        return EncodingType.ThriftRW;
      case ENCODING_TYPE_JSON:
        return EncodingType.JSON;
      case ENCODING_TYPE_PROTO3:
        throw new UnsupportedOperationException();
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static TimeoutType timeoutType(com.uber.cadence.api.v1.TimeoutType t) {
    switch (t) {
      case TIMEOUT_TYPE_INVALID:
        return null;
      case TIMEOUT_TYPE_START_TO_CLOSE:
        return TimeoutType.START_TO_CLOSE;
      case TIMEOUT_TYPE_SCHEDULE_TO_START:
        return TimeoutType.SCHEDULE_TO_START;
      case TIMEOUT_TYPE_SCHEDULE_TO_CLOSE:
        return TimeoutType.SCHEDULE_TO_CLOSE;
      case TIMEOUT_TYPE_HEARTBEAT:
        return TimeoutType.HEARTBEAT;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static DecisionTaskTimedOutCause decisionTaskTimedOutCause(
      com.uber.cadence.api.v1.DecisionTaskTimedOutCause t) {
    switch (t) {
      case DECISION_TASK_TIMED_OUT_CAUSE_INVALID:
        return null;
      case DECISION_TASK_TIMED_OUT_CAUSE_TIMEOUT:
        return DecisionTaskTimedOutCause.TIMEOUT;
      case DECISION_TASK_TIMED_OUT_CAUSE_RESET:
        return DecisionTaskTimedOutCause.RESET;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static CancelExternalWorkflowExecutionFailedCause
      cancelExternalWorkflowExecutionFailedCause(
          com.uber.cadence.api.v1.CancelExternalWorkflowExecutionFailedCause t) {
    switch (t) {
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return CancelExternalWorkflowExecutionFailedCause.UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
      case CANCEL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED:
        return CancelExternalWorkflowExecutionFailedCause.WORKFLOW_ALREADY_COMPLETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static SignalExternalWorkflowExecutionFailedCause
      signalExternalWorkflowExecutionFailedCause(
          com.uber.cadence.api.v1.SignalExternalWorkflowExecutionFailedCause t) {
    switch (t) {
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION:
        return SignalExternalWorkflowExecutionFailedCause.UNKNOWN_EXTERNAL_WORKFLOW_EXECUTION;
      case SIGNAL_EXTERNAL_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_COMPLETED:
        return SignalExternalWorkflowExecutionFailedCause.WORKFLOW_ALREADY_COMPLETED;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }

  public static ChildWorkflowExecutionFailedCause childWorkflowExecutionFailedCause(
      com.uber.cadence.api.v1.ChildWorkflowExecutionFailedCause t) {
    switch (t) {
      case CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_INVALID:
        return null;
      case CHILD_WORKFLOW_EXECUTION_FAILED_CAUSE_WORKFLOW_ALREADY_RUNNING:
        return ChildWorkflowExecutionFailedCause.WORKFLOW_ALREADY_RUNNING;
    }
    throw new IllegalArgumentException("unexpected enum value");
  }
}
