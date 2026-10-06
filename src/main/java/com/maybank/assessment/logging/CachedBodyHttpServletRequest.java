package com.maybank.assessment.logging;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads the request body up front so it can be logged BEFORE the request is processed,
 * while still letting the controller read it again.
 * Form and multipart requests are left untouched, because the container parses them from the stream.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = isCacheable(request.getContentType())
                ? StreamUtils.copyToByteArray(request.getInputStream())
                : null;
    }

    /** The cached body, or {@code null} when the body was not cached (form / multipart). */
    public byte[] getCachedBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (body == null) {
            return super.getInputStream();
        }
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Async reads are not supported");
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (body == null) {
            return super.getReader();
        }
        String encoding = getCharacterEncoding();
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    private static boolean isCacheable(String contentType) {
        if (contentType == null) {
            return true;
        }
        String type = contentType.toLowerCase();
        return !type.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)
                && !type.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }
}
