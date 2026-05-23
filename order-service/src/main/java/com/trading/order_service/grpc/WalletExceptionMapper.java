package com.trading.order_service.grpc;

import com.trading.order_service.exception.InsufficientAmount;
import com.trading.order_service.exception.WalletNotFound;
import io.grpc.StatusRuntimeException;

public class WalletExceptionMapper {
    public static RuntimeException map(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND -> new WalletNotFound(e.getStatus().getDescription());
            case INVALID_ARGUMENT -> new InsufficientAmount(e.getStatus().getDescription());
            default -> new RuntimeException(e);
        };
    }
}
