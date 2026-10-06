package com.maybank.assessment.service.impl;

import com.maybank.assessment.client.ExchangeRateClient;
import com.maybank.assessment.client.dto.ExchangeRateApiResponse;
import com.maybank.assessment.dto.response.CurrencyConversionResponse;
import com.maybank.assessment.dto.response.CustomerResponse;
import com.maybank.assessment.entity.CustomerStatus;
import com.maybank.assessment.exception.InvalidRequestException;
import com.maybank.assessment.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    private static final ExchangeRateApiResponse MYR_RATES = new ExchangeRateApiResponse(
            "success", "https://www.exchangerate-api.com", "Tue, 06 Oct 2026 00:02:31 +0000", "MYR",
            Map.of("MYR", BigDecimal.ONE, "USD", new BigDecimal("0.2125"), "SGD", new BigDecimal("0.2871")),
            null);

    @Test
    void convertCustomerBalance_multipliesBalanceByRate() {
        when(customerService.getCustomer(1L)).thenReturn(customer(new BigDecimal("1000.00")));
        when(exchangeRateClient.getLatestRates("MYR")).thenReturn(MYR_RATES);

        CurrencyConversionResponse result = exchangeRateService.convertCustomerBalance(1L, "usd");

        assertThat(result.toCurrency()).isEqualTo("USD");
        assertThat(result.exchangeRate()).isEqualByComparingTo("0.2125");
        assertThat(result.convertedBalance()).isEqualByComparingTo("212.50");
    }

    @Test
    void convertCustomerBalance_rejectsUnknownTargetCurrency() {
        when(customerService.getCustomer(1L)).thenReturn(customer(BigDecimal.TEN));
        when(exchangeRateClient.getLatestRates("MYR")).thenReturn(MYR_RATES);

        assertThatThrownBy(() -> exchangeRateService.convertCustomerBalance(1L, "XYZ"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void convertCustomerBalance_rejectsMalformedCurrencyBeforeAnyCall() {
        assertThatThrownBy(() -> exchangeRateService.convertCustomerBalance(1L, "US"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getLatestRates_filtersBySymbols() {
        when(exchangeRateClient.getLatestRates("MYR")).thenReturn(MYR_RATES);

        Map<String, BigDecimal> rates = exchangeRateService.getLatestRates("myr", Set.of("usd"));

        assertThat(rates).containsOnlyKeys("USD");
    }

    private static CustomerResponse customer(BigDecimal balance) {
        return new CustomerResponse(1L, "Ali", "ali@example.com", "+60123456789", "514012345678",
                balance, "MYR", CustomerStatus.ACTIVE, null, null);
    }
}
