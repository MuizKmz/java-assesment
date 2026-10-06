package com.maybank.assessment.dto.request;

import com.maybank.assessment.entity.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must not exceed 150 characters")
        String email,

        @NotBlank(message = "phoneNo is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "phoneNo must be 9-15 digits, optionally starting with +")
        String phoneNo,

        @NotNull(message = "status is required (ACTIVE, INACTIVE, CLOSED)")
        CustomerStatus status
) {
}
