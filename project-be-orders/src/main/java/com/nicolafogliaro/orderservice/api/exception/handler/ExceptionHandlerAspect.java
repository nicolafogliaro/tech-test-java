package com.nicolafogliaro.orderservice.api.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Profile("!test")
public class ExceptionHandlerAspect {

    @Before("@annotation(org.springframework.web.bind.annotation.ExceptionHandler) && args(e) ")
    public void logException(Exception e) {
        log.error("*** {}", e.getMessage(), e);
    }

}
