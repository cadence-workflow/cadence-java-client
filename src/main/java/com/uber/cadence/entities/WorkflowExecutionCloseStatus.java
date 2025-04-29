package com.uber.cadence.entities;

/**
 * WorkflowExecutionCloseStatus represents the status of a workflow execution when it is closed.
 */
public enum WorkflowExecutionCloseStatus {
    COMPLETED,
    FAILED,
    CANCELED,
    TERMINATED,
    CONTINUED_AS_NEW,
    TIMED_OUT
}
