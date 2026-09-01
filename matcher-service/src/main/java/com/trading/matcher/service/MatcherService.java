package com.trading.matcher.service;

import com.trading.matcher.model.*;
import com.trading.matcher.producer.TradeEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatcherService {
    private final Map<Market, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private TradeEventPublisher tradeEventPublisher;

    public MatcherService(TradeEventPublisher tradeEventPublisher) {
        this.tradeEventPublisher = tradeEventPublisher;
    }

    public void reset() {
        orderBooks.clear();
    }
    public void placeOrder(Order order) {
        Market market = new Market(order.getBaseAsset(), order.getQuoteAsset());
        OrderBook orderBook = orderBooks.computeIfAbsent(market, market1 -> new OrderBook());
        orderBook.getLock().lock();
        try {
            if (order.getSide().equals(OrderSide.BUY)) {
                matchBuy(orderBook, order);
                if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    orderBook.getBuyQueue().add(order);
                }
            } else {
                matchSell(orderBook, order);
                if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    orderBook.getSellQueue().add(order);
                }
            }
        } finally {
            orderBook.getLock().unlock();
        }
    }

    private void matchBuy(OrderBook orderBook, Order order) {
        while (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 && !orderBook.getSellQueue().isEmpty()) {
            Order sell = orderBook.getSellQueue().peek();
            if (sell == null
                || order.getPrice().compareTo(sell.getPrice()) < 0
//                || sell.getUserId().equals(order.getUserId())
            ) {
                break;
            }
            BigDecimal tradedQuantity = order.getRemainingQuantity().min(sell.getRemainingQuantity());

            order.setRemainingQuantity(order.getRemainingQuantity().subtract(tradedQuantity));
            sell.setRemainingQuantity(sell.getRemainingQuantity().subtract(tradedQuantity));

            // send kafka event
            System.out.println("buy trade executed " + tradedQuantity + "buy id: " + order.getOrderId() + "sell order id: " + sell.getOrderId());
           sendEvent(order, sell, tradedQuantity, sell.getPrice()); // comment when performing performance testing

            if (sell.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                orderBook.getSellQueue().poll();
            }
        }
    }

    private void matchSell(OrderBook orderBook, Order order) {
        while (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 && !orderBook.getBuyQueue().isEmpty()) {
            Order buy = orderBook.getBuyQueue().peek();
            if (buy == null
                    || buy.getPrice().compareTo(order.getPrice()) < 0
//                    || buy.getUserId().equals(order.getUserId())
            ) {
                break;
            }
            BigDecimal tradedQuantity = order.getRemainingQuantity().min(buy.getRemainingQuantity());

            order.setRemainingQuantity(order.getRemainingQuantity().subtract(tradedQuantity));
            buy.setRemainingQuantity(buy.getRemainingQuantity().subtract(tradedQuantity));

            // send kafka event
            System.out.println("sell trade executed " + tradedQuantity + "sell id: " + order.getOrderId() + "buy order id: " + buy.getOrderId());
            sendEvent(buy, order, tradedQuantity, buy.getPrice()); // comment when performing performance testing

            if (buy.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                orderBook.getBuyQueue().poll();
            }
        }
    }

    private void sendEvent(Order buy, Order sell, BigDecimal quantity, BigDecimal price) {
        tradeEventPublisher.publish(
                new TradeExecutedEvent(
                        buy.getOrderId(),
                        sell.getOrderId(),
                        buy.getUserId(),
                        sell.getUserId(),
                        buy.getBaseAsset(),
                        buy.getQuoteAsset(),
                        quantity.toString(),
                        price.toString()
                )
        );
    }
}
