package com.maybank.assessment.dto.response;

import java.math.BigDecimal;

public record CurrencyConversionResponse(
        Long customerId,
        String accountNo,
        String fromCurrency,
        BigDecimal balance,
        String toCurrency,
        BigDecimal exchangeRate,
        BigDecimal convertedBalance,
        String rateLastUpdatedUtc,
        String rateProvider
) {
}
