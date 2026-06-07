package com.trading.matcher.model;

public record TradeExecutedEvent(
        Long buyOrderId,
        Long sellOrderId,
        String baseAsset,
        String quoteAsset,
        String quantity,
        String price
) {}
