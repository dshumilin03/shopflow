package com.shopflow.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Generates new correlationId when header is missing")
    void generatesNewCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        AtomicReference<String> headerCaptured = new AtomicReference<>();

        doAnswer(inv -> {
            headerCaptured.set(inv.getArgument(1));
            return null;
        }).when(response).setHeader(eq("X-Correlation-Id"), anyString());

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        doAnswer(inv -> {
            mdcDuringChain.set(MDC.get("correlationId"));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilterInternal(request, response, chain);

        assertThat(headerCaptured.get()).isNotNull().isNotBlank();

        assertThat(mdcDuringChain.get()).isNotNull().isNotBlank();
        assertThat(mdcDuringChain.get()).isEqualTo(headerCaptured.get());

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Reuses existing correlationId from request header")
    void reusesExistingCorrelationId() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-Id")).thenReturn("abc-123");

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            mdcDuringChain.set(MDC.get("correlationId"));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Correlation-Id", "abc-123");

        assertThat(mdcDuringChain.get()).isEqualTo("abc-123");

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Removes correlationId from MDC after processing")
    void removesCorrelationIdAfterProcessing() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-Id")).thenReturn("xyz-999");

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Does not crash when chain.doFilter throws exception")
    void cleansMdcEvenIfExceptionThrown() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-Id")).thenReturn("boom");
        doThrow(new RuntimeException("Test Exception")).when(chain).doFilter(any(), any());

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (RuntimeException ignored) {}

        assertThat(MDC.get("correlationId")).isNull();
    }
}
