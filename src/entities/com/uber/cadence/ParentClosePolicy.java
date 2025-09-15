package com.uber.cadence;

public enum ParentClosePolicy {
  ABANDON,
  REQUEST_CANCEL,
  TERMINATE,
}
