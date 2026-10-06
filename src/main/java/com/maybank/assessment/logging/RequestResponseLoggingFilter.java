package com.maybank.assessment.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.maybank.assessment.logging.LoggingConstants.CORRELATION_ID_HEADER;
import static com.maybank.assessment.logging.LoggingConstants.CORRELATION_ID_MDC_KEY;
import static com.maybank.assessment.logging.LoggingConstants.MAX_LOGGED_BODY_LENGTH;

/**
 * Logs the REQUEST and RESPONSE (method, URI, headers, body, status, duration) of every API call.
 * Output goes to logs/api-request-response.log and logs/app.log (see logback-spring.xml).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "cookie", "set-cookie", "x-api-key");
    private static final List<String> EXCLUDED_PATH_PREFIXES = List.of("/swagger-ui", "/v3/api-docs", "/favicon.ico");
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return EXCLUDED_PATH_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, MAX_LOGGED_BODY_LENGTH);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(requestWrapper);
            logResponse(requestWrapper, responseWrapper, duration);
            // Body was buffered by the wrapper; it must be written back to the real response.
            responseWrapper.copyBodyToResponse();
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        log.info(">>> REQUEST  | {} {} | client={} | headers={} | body={}",
                request.getMethod(),
                fullUri(request),
                request.getRemoteAddr(),
                requestHeaders(request),
                bodyAsString(request.getContentAsByteArray(), request.getContentType(), request.getCharacterEncoding()));
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        log.info("<<< RESPONSE | {} {} | status={} | duration={}ms | headers={} | body={}",
                request.getMethod(),
                fullUri(request),
                response.getStatus(),
                duration,
                responseHeaders(response),
                bodyAsString(response.getContentAsByteArray(), response.getContentType(), response.getCharacterEncoding()));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        // Only trust well-formed client values, otherwise generate one (prevents log injection).
        return (incoming != null && SAFE_CORRELATION_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();
    }

    private static String fullUri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private static Map<String, String> requestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, mask(name, request.getHeader(name)));
        }
        return headers;
    }

    private static Map<String, String> responseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, mask(name, response.getHeader(name)));
        }
        if (response.getContentType() != null) {
            headers.putIfAbsent("Content-Type", response.getContentType());
        }
        return headers;
    }

    private static String mask(String headerName, String value) {
        return MASKED_HEADERS.contains(headerName.toLowerCase()) ? "******" : value;
    }

    static String bodyAsString(byte[] content, String contentType, String encoding) {
        if (content == null || content.length == 0) {
            return "<empty>";
        }
        if (!isTextual(contentType)) {
            return "<binary content, " + content.length + " bytes>";
        }
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        String body = new String(content, charset).replaceAll("\\s*[\\r\\n]+\\s*", " ");
        return body.length() > MAX_LOGGED_BODY_LENGTH
                ? body.substring(0, MAX_LOGGED_BODY_LENGTH) + "...<truncated>"
                : body;
    }

    private static boolean isTextual(String contentType) {
        if (contentType == null) {
            return true;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return "text".equals(mediaType.getType())
                    || mediaType.getSubtype().contains("json")
                    || mediaType.getSubtype().contains("xml")
                    || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
        } catch (Exception ex) {
            return false;
        }
    }
}
