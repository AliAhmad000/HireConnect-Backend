package com.hireconnect.job.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.hireconnect.job.service..*) || within(com.hireconnect.job.controller..*)")
    public void applicationPointcut() {}

    @Around("applicationPointcut()")
    public Object logAround(ProceedingJoinPoint jp) throws Throwable {
        long start = System.currentTimeMillis();
        log.debug("Entering: {}.{}()",
                jp.getSignature().getDeclaringTypeName(), jp.getSignature().getName());
        try {
            Object result = jp.proceed();
            log.debug("Exiting: {}.{}() in {}ms",
                    jp.getSignature().getDeclaringTypeName(),
                    jp.getSignature().getName(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("Exception in {}.{}(): {}",
                    jp.getSignature().getDeclaringTypeName(),
                    jp.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
