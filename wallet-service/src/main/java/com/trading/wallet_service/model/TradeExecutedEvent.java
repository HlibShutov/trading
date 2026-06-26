package com.trading.wallet_service.model;

public record TradeExecutedEvent(
        Long buyOrderId,
        Long sellOrderId,
        Long buyerUserId,
        Long sellerUserId,
        String baseAsset,
        String quoteAsset,
        String quantity,
        String price
) {}
