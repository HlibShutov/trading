package com.trading.wallet_service.consumer;

import com.trading.wallet_service.model.TradeExecutedEvent;
import com.trading.wallet_service.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TradeEventConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WalletService walletService;

    public TradeEventConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = "trade-executed", groupId = "wallet-service")
    public void listen(String payload) {
        TradeExecutedEvent trade = objectMapper.readValue(payload, TradeExecutedEvent.class);
        walletService.processTrade(trade);
    }
}
