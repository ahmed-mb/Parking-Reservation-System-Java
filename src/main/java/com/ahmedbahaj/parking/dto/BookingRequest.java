package com.ahmedbahaj.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingRequest {
    
    @NotNull(message = "User ID is required")
    private Integer userId;
    
    @NotBlank(message = "Parking ID is required")
    @Pattern(regexp = "^[A-Z]-\\d{3}$", message = "Invalid parking spot format (e.g., A-001)")
    private String parkingId;
    
    @NotBlank(message = "Car plate is required")
    @Size(max = 20, message = "Car plate must not exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\- ]+$", message = "Invalid car plate format")
    private String carPlate;
}
