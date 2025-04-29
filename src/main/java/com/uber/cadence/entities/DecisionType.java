package com.uber.cadence.entities;

/**
 * DecisionType represents the type of decision that can be made in a workflow.
 */
public enum DecisionType {
    /**
     * Schedule an activity task.
     */
    ScheduleActivityTask,

    /**
     * Request cancellation of an activity task.
     */
    RequestCancelActivityTask,

    /**
     * Start a timer.
     */
    StartTimer,

    /**
     * Complete the workflow execution.
     */
    CompleteWorkflowExecution,

    /**
     * Fail the workflow execution.
     */
    FailWorkflowExecution,

    /**
     * Cancel a timer.
     */
    CancelTimer,

    /**
     * Cancel the workflow execution.
     */
    CancelWorkflowExecution,

    /**
     * Request cancellation of an external workflow execution.
     */
    RequestCancelExternalWorkflowExecution,

    /**
     * Record a marker in the workflow history.
     */
    RecordMarker,

    /**
     * Continue the workflow execution as new.
     */
    ContinueAsNewWorkflowExecution,

    /**
     * Start a child workflow execution.
     */
    StartChildWorkflowExecution,

    /**
     * Signal an external workflow execution.
     */
    SignalExternalWorkflowExecution,

    /**
     * Upsert workflow search attributes.
     */
    UpsertWorkflowSearchAttributes
}
