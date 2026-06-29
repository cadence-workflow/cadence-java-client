package com.uber.cadence;

public enum ScheduleOverlapPolicy {
  INVALID,
  SKIP_NEW,
  BUFFER,
  CONCURRENT,
  CANCEL_PREVIOUS,
  TERMINATE_PREVIOUS,
}
