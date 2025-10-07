package com.shopflow.user.service.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@Order(2)
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    private final LoggingSanitizer sanitizer;

    @Pointcut("execution(* com.shopflow.user.controller..*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        HttpServletRequest request = getCurrentRequest();
        HttpServletResponse response = getCurrentResponse();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString() != null
                ? "?" + request.getQueryString()
                : "";


        String correlationId = MDC.get("correlationId");
        String handler = joinPoint.getSignature().toShortString();

        Object[] args = joinPoint.getArgs();
        String sanitizedArgs = sanitizer.sanitize(args);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();

            log.info(
                    "[Controller] {} {}{} | status={} | duration={}ms | correlationId={} | handler={} | args={}",
                    method, uri, query, status, duration, correlationId, handler, sanitizedArgs
            );

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

            log.error(
                    "[Controller][ERROR] {} {}{} | status={} | duration={}ms | correlationId={} | handler={} | error={} | args={}",
                    method, uri, query, status, duration, correlationId, handler, e.getMessage(), sanitizedArgs
            );
            throw e;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No request attributes found — ControllerLoggingAspect called outside of HTTP request context");
        }
        return attrs.getRequest();
    }

    private HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No request attributes found — ControllerLoggingAspect called outside of HTTP request context");
        }
        return attrs.getResponse();
    }

}
