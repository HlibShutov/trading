package com.trading.order_service.consumer;

import com.trading.order_service.model.TradeExecutedEvent;
import com.trading.order_service.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TradeEventConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TradeEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    private final OrderService orderService;

    @KafkaListener(topics = "trade-executed", groupId = "order-service")
    public void listen(String payload) {
        TradeExecutedEvent trade = objectMapper.readValue(payload, TradeExecutedEvent.class);
        orderService.processTrade(trade);
    }
}
