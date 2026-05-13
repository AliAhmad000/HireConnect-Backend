package com.hireconnect.interview.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect @Component @Slf4j
public class LoggingAspect {
    @Pointcut("within(com.hireconnect.interview.service..*) || within(com.hireconnect.interview.controller..*)")
    public void applicationPointcut() {}

    @Around("applicationPointcut()")
    public Object logAround(ProceedingJoinPoint jp) throws Throwable {
        long start = System.currentTimeMillis();
        log.debug("Entering {}.{}()", jp.getSignature().getDeclaringTypeName(), jp.getSignature().getName());
        try {
            Object result = jp.proceed();
            log.debug("Exiting {}.{}() in {}ms", jp.getSignature().getDeclaringTypeName(),
                    jp.getSignature().getName(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("Exception in {}.{}(): {}", jp.getSignature().getDeclaringTypeName(),
                    jp.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
