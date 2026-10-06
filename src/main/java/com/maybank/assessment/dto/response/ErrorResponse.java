package com.maybank.assessment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error envelope produced by the global exception handler.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors,
        OffsetDateTime timestamp
) {
}
