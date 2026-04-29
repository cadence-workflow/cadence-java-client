package com.uber.cadence;

public enum WorkflowExecutionStatus {
  PENDING,
  STARTED,
  COMPLETED,
  FAILED,
  CANCELED,
  TERMINATED,
  CONTINUED_AS_NEW,
  TIMED_OUT,
}
