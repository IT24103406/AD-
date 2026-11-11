package com.ridelink.ridemanagementservice.model;

/**
 * Enum representing all possible states of a ride in its lifecycle.
 *
 * Lifecycle:
 * REQUESTED -> DRIVER_ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 *
 * Cancellation is allowed from: REQUESTED, DRIVER_ASSIGNED, ACCEPTED
 * Cancellation is NOT allowed from: IN_PROGRESS, COMPLETED, CANCELLED
 */
public enum RideStatus {
    REQUESTED,
    DRIVER_ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
