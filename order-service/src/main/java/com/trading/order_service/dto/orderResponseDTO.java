package com.trading.order_service.dto;

import com.trading.order_service.model.OrderStatus;

public record orderResponseDTO(Long orderId, OrderStatus status) {
}
