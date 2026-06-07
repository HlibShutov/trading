package com.trading.matcher.model;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class Order {
    private Long orderId;
    private Long userId;
    private OrderSide side;
    private String baseAsset;
    private String quoteAsset;
    private BigDecimal remainingQuantity;
    private BigDecimal price;
}
