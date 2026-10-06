package com.maybank.assessment.service;

import com.maybank.assessment.dto.response.CurrencyConversionResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface ExchangeRateService {

    Map<String, BigDecimal> getLatestRates(String baseCurrency, Set<String> symbols);

    CurrencyConversionResponse convertCustomerBalance(Long customerId, String targetCurrency);
}
