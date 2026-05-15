package com.trading.order_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    @GetMapping("/order")
    public String getOrder(@RequestParam long userId) {
        return orderService.getOrder(userId);
    }
}
