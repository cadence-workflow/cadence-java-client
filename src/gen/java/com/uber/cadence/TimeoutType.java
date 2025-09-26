package com.uber.cadence;

public enum TimeoutType {
  START_TO_CLOSE,
  SCHEDULE_TO_START,
  SCHEDULE_TO_CLOSE,
  HEARTBEAT,
}
