package com.uber.cadence.entities;

/**
 * DomainStatus represents the status of a domain.
 */
public enum DomainStatus {
    /**
     * Domain is registered and active.
     */
    REGISTERED,

    /**
     * Domain is deprecated.
     */
    DEPRECATED,

    /**
     * Domain is deleted.
     */
    DELETED
}
