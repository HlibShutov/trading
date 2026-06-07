package com.trading.order_service.controller;

import com.trading.order_service.dto.orderRequestDTO;
import com.trading.order_service.dto.orderResponseDTO;
import com.trading.order_service.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    @PostMapping("/order")
    public orderResponseDTO createOrder(@RequestBody orderRequestDTO orderRequest) {
        return orderService.createOrder(orderRequest);
    }
}
