package com.maybank.assessment.exception;

/**
 * Raised when a downstream 3rd-party API is unreachable or returns an unexpected response.
 */
public class ThirdPartyServiceException extends RuntimeException {

    public ThirdPartyServiceException(String message) {
        super(message);
    }

    public ThirdPartyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
