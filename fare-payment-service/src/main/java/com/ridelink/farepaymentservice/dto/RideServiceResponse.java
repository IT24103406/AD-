package com.ridelink.farepaymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RideServiceResponse {
    private String id;
    private String passengerAccountId;
    private String status;
}
