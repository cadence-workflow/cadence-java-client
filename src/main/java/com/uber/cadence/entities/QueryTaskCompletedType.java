package com.uber.cadence.entities;

/**
 * QueryTaskCompletedType represents the type of completion for a query task.
 */
public enum QueryTaskCompletedType {
    /**
     * Query task completed successfully.
     */
    COMPLETED,

    /**
     * Query task failed.
     */
    FAILED,

    /**
     * Query task was rejected.
     */
    REJECTED
}
