package com.trading.matcher.grpcserver;

import com.trading.matcher.grpc.*;
import com.trading.matcher.model.Order;
import com.trading.matcher.model.OrderSide;
import com.trading.matcher.service.MatcherService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;

@GrpcService
public class MatcherGrpcService extends MatcherServiceGrpc.MatcherServiceImplBase {
    private final MatcherService matcherService;

    public MatcherGrpcService(MatcherService matcherService) {
        this.matcherService = matcherService;
    }

    @Override
    public void reset(
            ResetRequest request,
            StreamObserver<ResetResponse> responseObserver) {

        matcherService.reset();

        responseObserver.onNext(
                ResetResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void placeOrder(PlaceOrderRequest request, StreamObserver<PlaceOrderResponse> responseObserver) {
        Order order = new Order(
                request.getOrderId(),
                request.getUserId(),
                toDomain(request.getSide()),
                request.getBase(),
                request.getQuote(),
                new BigDecimal(request.getQuantity()),
                new BigDecimal(request.getPrice())
        );
        matcherService.placeOrder(order);

        PlaceOrderResponse response = PlaceOrderResponse.newBuilder()
                .setAccepted(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    private OrderSide toDomain(com.trading.matcher.grpc.OrderSide grpcSide) {
        return switch (grpcSide) {
            case BUY -> OrderSide.BUY;
            case SELL -> OrderSide.SELL;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown order side: " + grpcSide);
        };
    }
}
