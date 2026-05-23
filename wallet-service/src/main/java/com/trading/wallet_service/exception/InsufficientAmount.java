package com.trading.wallet_service.exception;

public class InsufficientAmount extends RuntimeException {
    public InsufficientAmount(String message) {
        super(message);
    }
}
