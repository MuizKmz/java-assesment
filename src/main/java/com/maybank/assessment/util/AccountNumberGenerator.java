package com.maybank.assessment.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates 12-digit account numbers: fixed 4-digit branch prefix + 8 random digits.
 */
@Component
public class AccountNumberGenerator {

    private static final String BRANCH_PREFIX = "5140";
    private static final int RANDOM_DIGITS = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(BRANCH_PREFIX);
        for (int i = 0; i < RANDOM_DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
