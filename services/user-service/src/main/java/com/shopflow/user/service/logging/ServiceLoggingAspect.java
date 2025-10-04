package com.shopflow.user.service.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(3)
@RequiredArgsConstructor
public class ServiceLoggingAspect {

    private final LoggingSanitizer sanitizer;

    @Around("execution(* com.shopflow.user.service..*(..)) && !within(com.shopflow.user.service.logging..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String methodName = joinPoint.getSignature().getName();
        String sanitizedArgs = sanitizer.sanitize(joinPoint.getArgs());

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            String sanitizedResult = sanitizer.sanitize(new Object[]{result});

            log.info(
                    "[Service] {} | correlationId={} | status=SUCCESS | duration={}ms | args={} | result={}",
                    methodName, correlationId, duration, sanitizedArgs, sanitizedResult
            );
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error(
                    "[Service][ERROR] {} | correlationId={} | status=FAILED | duration={}ms | error={} | args={}",
                    methodName, correlationId, duration, e.getMessage(), sanitizedArgs, e
            );
            throw e;
        }
    }
}
