package com.maybank.assessment.dto.response;

import com.maybank.assessment.entity.CustomerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String fullName,
        String email,
        String phoneNo,
        String accountNo,
        BigDecimal balance,
        String currency,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
