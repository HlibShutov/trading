package com.trading.wallet_service.grpc;

import com.trading.wallet_service.exception.InsufficientAmount;
import com.trading.wallet_service.exception.WalletRecordNotFound;
import com.trading.wallet_service.service.WalletService;
import com.trading.walletservice.grpc.ReserveFundsRequest;
import com.trading.walletservice.grpc.ReserveFundsResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class WalletGrpcService extends com.trading.walletservice.grpc.WalletServiceGrpc.WalletServiceImplBase {

    public WalletGrpcService(WalletService walletService) {
        this.walletService = walletService;
    }

    private final WalletService walletService;


    @Override
    public void reserveFunds(
            ReserveFundsRequest request,
            StreamObserver<ReserveFundsResponse> responseObserver
    ) {
        try {
            walletService.reserveFunds(request);
        } catch (WalletRecordNotFound e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (InsufficientAmount e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }

        ReserveFundsResponse response = ReserveFundsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Funds reserved")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}