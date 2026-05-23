package com.trading.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason="Insufficient amount")
public class InsufficientAmount extends RuntimeException {
    public InsufficientAmount(String message) {
        super(message);
    }
}
