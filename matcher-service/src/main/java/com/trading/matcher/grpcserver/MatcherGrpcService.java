package com.trading.matcher.grpcserver;

import com.trading.matcher.grpc.MatcherServiceGrpc;
import com.trading.matcher.grpc.PlaceOrderRequest;
import com.trading.matcher.grpc.PlaceOrderResponse;
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
