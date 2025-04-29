package com.uber.cadence.entities;

public enum WorkflowIdReusePolicy {
    AllowDuplicateFailedOnly,
    AllowDuplicate,
  RejectDuplicate,
  TerminateIfRunning;
}
