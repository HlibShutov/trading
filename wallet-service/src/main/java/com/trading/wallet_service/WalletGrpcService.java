package com.trading.wallet_service;

import com.trading.walletservice.grpc.ReserveFundsRequest;
import com.trading.walletservice.grpc.ReserveFundsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class WalletGrpcService extends com.trading.walletservice.grpc.WalletServiceGrpc.WalletServiceImplBase {

    @Override
    public void reserveFunds(
            ReserveFundsRequest request,
            StreamObserver<ReserveFundsResponse> responseObserver
    ) {

        System.out.println(String.format("Reserve funds request received for %d", request.getUserId()));

        ReserveFundsResponse response = ReserveFundsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Funds reserved")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}