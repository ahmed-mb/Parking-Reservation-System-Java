package com.ahmedbahaj.parking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
    
    @Size(max = 20, message = "Mobile number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Invalid mobile number format")
    private String mobile;
    
    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;
    
    @Size(max = 20, message = "Car plate number must not exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\- ]*$", message = "Invalid car plate format")
    private String carPlateNo;
    
    @NotBlank(message = "reCAPTCHA token is required")
    private String recaptchaToken;
}
