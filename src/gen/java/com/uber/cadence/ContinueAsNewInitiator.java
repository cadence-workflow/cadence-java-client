package com.uber.cadence;

public enum ContinueAsNewInitiator {
  Decider,
  RetryPolicy,
  CronSchedule,
}
