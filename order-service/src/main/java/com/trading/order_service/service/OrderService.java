package com.trading.order_service.service;

import com.trading.order_service.dto.orderRequestDTO;
import com.trading.order_service.dto.orderResponseDTO;
import com.trading.order_service.grpc.WalletExceptionMapper;
import com.trading.order_service.model.Order;
import com.trading.order_service.model.OrderSide;
import com.trading.order_service.model.OrderStatus;
import com.trading.order_service.repository.OrderRepository;
import com.trading.walletservice.grpc.ReserveFundsRequest;
import com.trading.walletservice.grpc.WalletServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;

import java.math.BigDecimal;

@Service
public class OrderService {
    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletServiceBlockingStub;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private final OrderRepository orderRepository;

    public String getOrder(long userId) {
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setUserId(userId)
                .setAmount("100.0")
                .setAsset("BTC")
                .build();
        return walletServiceBlockingStub.reserveFunds(request).getMessage();
    }

    public orderResponseDTO createOrder(orderRequestDTO orderRequest) {
        BigDecimal reserveAmount;
        String reserveAsset;

        if (orderRequest.side() == OrderSide.BUY) {
            reserveAmount = orderRequest.quantity().multiply(orderRequest.price());
            reserveAsset = orderRequest.quote();
        } else {
            reserveAmount = orderRequest.quantity();
            reserveAsset = orderRequest.base();
        }
        ReserveFundsRequest request = ReserveFundsRequest.newBuilder()
                .setUserId(orderRequest.userId())
                .setAmount(reserveAmount.toString())
                .setAsset(reserveAsset)
                .build();
        try {
            walletServiceBlockingStub.reserveFunds(request);
        } catch (StatusRuntimeException e) {
            throw WalletExceptionMapper.map(e);
        }

        Order order = new Order(
                orderRequest.userId(),
                orderRequest.side(),
                OrderStatus.OPEN,
                orderRequest.quantity(),
                orderRequest.price(),
                orderRequest.base(),
                orderRequest.quote()
        );

        orderRepository.save(order);

        return new orderResponseDTO(orderRequest.userId(), order.getStatus());
    }
}