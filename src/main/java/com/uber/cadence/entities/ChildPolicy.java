package com.uber.cadence.entities;

public enum ChildPolicy {
    TERMINATE,
    REQUEST_CANCEL,
    ABANDON
}
