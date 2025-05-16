package com.nicolafogliaro.orderservice.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrencyConflictException extends RuntimeException { // For stockQuantity update conflicts
    public ConcurrencyConflictException(String message) {
        super(message);
    }
}