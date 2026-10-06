package com.maybank.assessment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response contract of the 3rd-party API https://open.er-api.com/v6/latest/{base}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse(
        String result,
        String provider,
        @JsonProperty("time_last_update_utc") String timeLastUpdateUtc,
        @JsonProperty("base_code") String baseCode,
        Map<String, BigDecimal> rates,
        @JsonProperty("error-type") String errorType
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(result);
    }
}
