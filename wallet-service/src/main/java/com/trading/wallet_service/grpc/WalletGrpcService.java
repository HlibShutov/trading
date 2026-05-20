package com.trading.wallet_service.grpc;

import com.trading.wallet_service.service.WalletService;
import com.trading.walletservice.grpc.ReserveFundsRequest;
import com.trading.walletservice.grpc.ReserveFundsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class WalletGrpcService extends com.trading.walletservice.grpc.WalletServiceGrpc.WalletServiceImplBase {

    public WalletGrpcService(WalletService walletService) {
        this.walletService = walletService;
    }

    private WalletService walletService;


    @Override
    public void reserveFunds(
            ReserveFundsRequest request,
            StreamObserver<ReserveFundsResponse> responseObserver
    ) {

        walletService.reserveFunds(request);

        ReserveFundsResponse response = ReserveFundsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Funds reserved")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}