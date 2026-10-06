package com.maybank.assessment.service.impl;

import com.maybank.assessment.client.ExchangeRateClient;
import com.maybank.assessment.client.dto.ExchangeRateApiResponse;
import com.maybank.assessment.dto.response.CurrencyConversionResponse;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.exception.InvalidRequestException;
import com.maybank.assessment.service.CustomerService;
import com.maybank.assessment.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Deliberately NOT @Transactional: the DB read is done inside CustomerService's own read-only
 * transaction, so no DB connection is held open while waiting on the 3rd-party HTTP call.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateClient exchangeRateClient;
    private final CustomerService customerService;

    @Override
    public Map<String, BigDecimal> getLatestRates(String baseCurrency, Set<String> symbols) {
        ExchangeRateApiResponse response = exchangeRateClient.getLatestRates(normalise(baseCurrency));

        Map<String, BigDecimal> rates = new TreeMap<>(response.rates());
        if (symbols != null && !symbols.isEmpty()) {
            Set<String> wanted = symbols.stream().map(ExchangeRateServiceImpl::normalise)
                    .collect(Collectors.toSet());
            rates.keySet().retainAll(wanted);
        }
        return rates;
    }

    @Override
    public CurrencyConversionResponse convertCustomerBalance(Long customerId, String targetCurrency) {
        String target = normalise(targetCurrency);

        // 1) Load from our DB (read-only transaction)
        CustomerResponse customer = customerService.getCustomer(customerId);

        // 2) Nested call to the 3rd-party API
        ExchangeRateApiResponse rates = exchangeRateClient.getLatestRates(customer.currency());
        BigDecimal rate = rates.rates().get(target);
        if (rate == null) {
            throw new InvalidRequestException("Unsupported target currency: " + target);
        }

        BigDecimal converted = customer.balance().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        log.info("Converted balance of customer id={} {} {} -> {} {} (rate={})",
                customerId, customer.balance(), customer.currency(), converted, target, rate);

        return new CurrencyConversionResponse(
                customer.id(),
                customer.accountNo(),
                customer.currency(),
                customer.balance(),
                target,
                rate,
                converted,
                rates.timeLastUpdateUtc(),
                rates.provider()
        );
    }

    private static String normalise(String currency) {
        if (currency == null || !currency.trim().matches("(?i)^[A-Z]{3}$")) {
            throw new InvalidRequestException("Currency must be a 3-letter ISO code, e.g. MYR, USD");
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
