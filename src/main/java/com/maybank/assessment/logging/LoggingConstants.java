package com.maybank.assessment.logging;

public final class LoggingConstants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /** Bodies larger than this are truncated in the log file. */
    public static final int MAX_LOGGED_BODY_LENGTH = 10_000;

    private LoggingConstants() {
    }
}
