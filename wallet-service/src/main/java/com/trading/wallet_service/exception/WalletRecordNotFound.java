package com.trading.wallet_service.exception;

public class WalletRecordNotFound extends RuntimeException {
    public WalletRecordNotFound(String message) {
        super(message);
    }
}
