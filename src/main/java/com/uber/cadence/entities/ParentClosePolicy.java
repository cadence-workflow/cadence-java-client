package com.uber.cadence.entities;

/**
 * ParentClosePolicy represents the policy for handling child workflows when the parent workflow is closed.
 */
public enum ParentClosePolicy {
    /**
     * Abandon the child workflow when parent is closed.
     */
    ABANDON,

    /**
     * Request cancellation of the child workflow when parent is closed.
     */
    REQUEST_CANCEL,

    /**
     * Terminate the child workflow when parent is closed.
     */
    TERMINATE
}
