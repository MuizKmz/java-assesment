package com.maybank.assessment.controller;

import com.maybank.assessment.dto.response.ApiResponse;
import com.maybank.assessment.dto.response.CurrencyConversionResponse;
import com.maybank.assessment.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Endpoints that make a nested call to the 3rd-party exchange rate API:
 * Client (Postman) -> this API -> https://open.er-api.com
 */
@Tag(name = "Exchange Rates", description = "Nested calls to the 3rd-party exchange rate API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "Convert a customer's balance to another currency (DB + 3rd-party call)")
    @GetMapping("/customers/{id}/balance/convert")
    public ResponseEntity<ApiResponse<CurrencyConversionResponse>> convertCustomerBalance(
            @PathVariable @Positive Long id,
            @RequestParam("to") String targetCurrency) {

        return ResponseEntity.ok(ApiResponse.success("Balance converted successfully",
                exchangeRateService.convertCustomerBalance(id, targetCurrency)));
    }

    @Operation(summary = "Get latest exchange rates for a base currency (pass-through to 3rd-party)")
    @GetMapping("/exchange-rates")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getLatestRates(
            @RequestParam(defaultValue = "MYR") String base,
            @RequestParam(required = false) Set<String> symbols) {

        return ResponseEntity.ok(ApiResponse.success("Exchange rates retrieved successfully",
                exchangeRateService.getLatestRates(base, symbols)));
    }
}
