package com.shopflow.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(1)
@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        try {
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new correlationId: {}", correlationId);
            } else {
                log.debug("Using existing correlationId: {}", correlationId);
            }

            MDC.put(CORRELATION_ID_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            if (MDC.get(CORRELATION_ID_KEY) != null) {
                MDC.remove(CORRELATION_ID_KEY);
            }
        }

    }
}
