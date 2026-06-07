package com.trading.order_service.consumer;

import com.trading.order_service.model.TradeExecutedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TradeEventConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "trade-executed", groupId = "order-service")
    public void listen(String payload) {
        System.out.println("got event");
        TradeExecutedEvent trade = objectMapper.readValue(payload, TradeExecutedEvent.class);
        System.out.println(trade);
    }
}
