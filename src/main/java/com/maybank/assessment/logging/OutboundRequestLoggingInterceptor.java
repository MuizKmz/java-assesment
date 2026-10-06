package com.maybank.assessment.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Logs the REQUEST and RESPONSE of outbound calls made to 3rd-party APIs.
 * Requires a buffering request factory so the response body can be read twice.
 */
@Slf4j
public class OutboundRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final String clientName;

    public OutboundRequestLoggingInterceptor(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        log.info(">>> 3RD-PARTY REQUEST  | [{}] {} {} | headers={} | body={}",
                clientName,
                request.getMethod(),
                request.getURI(),
                request.getHeaders(),
                body.length == 0 ? "<empty>" : new String(body, StandardCharsets.UTF_8));

        long start = System.currentTimeMillis();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - start;

            byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
            MediaType contentType = response.getHeaders().getContentType();
            log.info("<<< 3RD-PARTY RESPONSE | [{}] {} {} | status={} | duration={}ms | body={}",
                    clientName,
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode().value(),
                    duration,
                    RequestResponseLoggingFilter.bodyAsString(responseBody,
                            contentType != null ? contentType.toString() : null, null));
            return response;
        } catch (IOException ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("<<< 3RD-PARTY ERROR    | [{}] {} {} | duration={}ms | error={}",
                    clientName, request.getMethod(), request.getURI(), duration, ex.toString());
            throw ex;
        }
    }
}
