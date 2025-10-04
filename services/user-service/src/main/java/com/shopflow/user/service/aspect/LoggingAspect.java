package com.shopflow.user.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.shopflow.user.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("correlationId: {}, action: {}, status: success, duration: {} ms", correlationId, methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("correlationId: {}, action: {}, status: failed, error: {} , duration: {} ms", correlationId, methodName, e.getMessage(), duration);
            throw e;
        }
    }
}
