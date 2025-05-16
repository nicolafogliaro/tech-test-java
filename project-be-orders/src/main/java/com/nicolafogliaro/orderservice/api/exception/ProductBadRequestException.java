package com.nicolafogliaro.orderservice.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested product is not found in the system.
 * Results in an HTTP 404 Not Found response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductBadRequestException extends RuntimeException {
    public ProductBadRequestException(String message) {super(message);}
}
