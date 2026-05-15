package com.trading.order_service;

import com.trading.walletservice.grpc.ReserveFundsRequest;
import com.trading.walletservice.grpc.WalletServiceGrpc;
import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;

@Service
public class OrderService {
    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletServiceBlockingStub;

    public String getOrder(long userId) {
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setUserId(userId)
                .setAmount("100.0")
                .setAsset("BTC")
                .build();
        return walletServiceBlockingStub.reserveFunds(request).getMessage();
    }
}
