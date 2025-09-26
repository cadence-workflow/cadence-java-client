package com.uber.cadence;

public enum GetTaskFailedCause {
  SERVICE_BUSY,
  TIMEOUT,
  SHARD_OWNERSHIP_LOST,
  UNCATEGORIZED,
}
