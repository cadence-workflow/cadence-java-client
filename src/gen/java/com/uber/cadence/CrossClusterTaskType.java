package com.uber.cadence;

public enum CrossClusterTaskType {
  StartChildExecution,
  CancelExecution,
  SignalExecution,
  RecordChildWorkflowExecutionComplete,
  ApplyParentClosePolicy,
}
