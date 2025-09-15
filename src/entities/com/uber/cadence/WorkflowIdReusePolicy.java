package com.uber.cadence;

public enum WorkflowIdReusePolicy {
  AllowDuplicateFailedOnly,
  AllowDuplicate,
  RejectDuplicate,
  TerminateIfRunning,
}
