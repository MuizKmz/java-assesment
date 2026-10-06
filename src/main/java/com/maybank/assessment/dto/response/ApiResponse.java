package com.maybank.assessment.dto.response;

import java.time.OffsetDateTime;

/**
 * Standard success envelope returned by every endpoint.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }
}
