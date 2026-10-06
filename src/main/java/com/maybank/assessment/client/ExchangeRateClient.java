package com.maybank.assessment.client;

import com.maybank.assessment.client.dto.ExchangeRateApiResponse;
import com.maybank.assessment.exception.InvalidRequestException;
import com.maybank.assessment.exception.ThirdPartyServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP client for the 3rd-party exchange rate API (https://open.er-api.com).
 */
@Component
public class ExchangeRateClient {

    private static final String UNSUPPORTED_CODE = "unsupported-code";

    private final RestClient restClient;

    public ExchangeRateClient(@Qualifier("exchangeRateRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ExchangeRateApiResponse getLatestRates(String baseCurrency) {
        ExchangeRateApiResponse response;
        try {
            response = restClient.get()
                    .uri("/latest/{base}", baseCurrency)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, resp) -> {
                        throw new ThirdPartyServiceException(
                                "Exchange rate provider returned HTTP " + resp.getStatusCode().value());
                    })
                    .body(ExchangeRateApiResponse.class);
        } catch (ThirdPartyServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ThirdPartyServiceException("Exchange rate provider is unavailable: " + ex.getMessage(), ex);
        }

        if (response == null) {
            throw new ThirdPartyServiceException("Exchange rate provider returned an empty response");
        }
        if (!response.isSuccess()) {
            if (UNSUPPORTED_CODE.equals(response.errorType())) {
                throw new InvalidRequestException("Unsupported currency code: " + baseCurrency);
            }
            throw new ThirdPartyServiceException("Exchange rate provider error: " + response.errorType());
        }
        return response;
    }
}
