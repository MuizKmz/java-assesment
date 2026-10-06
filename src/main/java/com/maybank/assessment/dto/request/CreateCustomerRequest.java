package com.maybank.assessment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCustomerRequest(

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

        @NotNull(message = "initialDeposit is required")
        @DecimalMin(value = "0.00", message = "initialDeposit must not be negative")
        @Digits(integer = 17, fraction = 2, message = "initialDeposit must have at most 2 decimal places")
        BigDecimal initialDeposit,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code, e.g. MYR")
        String currency
) {
}
