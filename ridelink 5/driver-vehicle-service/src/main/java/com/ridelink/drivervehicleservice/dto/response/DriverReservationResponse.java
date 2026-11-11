package com.ridelink.drivervehicleservice.dto.response;

/**
 * Result of an atomic driver reservation attempt.
 *
 * reserved = true only when the driver was atomically flipped AVAILABLE -> ON_RIDE
 * AND an ACTIVE vehicle was resolved for that driver. When reserved = false, the driver
 * was either already taken by another request or had no ACTIVE vehicle (in which case the
 * driver was released back to AVAILABLE). vehicleId is null unless reserved is true.
 */
public record DriverReservationResponse(
        boolean reserved,
        String driverId,
        String vehicleId,
        String reason
) {
    public static DriverReservationResponse success(String driverId, String vehicleId) {
        return new DriverReservationResponse(true, driverId, vehicleId, null);
    }

    public static DriverReservationResponse failure(String driverId, String reason) {
        return new DriverReservationResponse(false, driverId, null, reason);
    }
}
