package com.trading.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="Wallet not found")
public class WalletNotFound extends RuntimeException {
    public WalletNotFound(String message) {
        super(message);
    }
}
