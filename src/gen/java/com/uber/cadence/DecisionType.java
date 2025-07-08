package com.uber.cadence;

public enum DecisionType {
  ScheduleActivityTask,
  RequestCancelActivityTask,
  StartTimer,
  CompleteWorkflowExecution,
  FailWorkflowExecution,
  CancelTimer,
  CancelWorkflowExecution,
  RequestCancelExternalWorkflowExecution,
  RecordMarker,
  ContinueAsNewWorkflowExecution,
  StartChildWorkflowExecution,
  SignalExternalWorkflowExecution,
  UpsertWorkflowSearchAttributes,
}
