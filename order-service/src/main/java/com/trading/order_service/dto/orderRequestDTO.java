package com.trading.order_service.dto;

import com.trading.order_service.model.OrderSide;

import java.math.BigDecimal;

public record orderRequestDTO(
        Long userId,
        String base,
        String quote,
        BigDecimal quantity,
        BigDecimal price,
        OrderSide side
) { }
