package com.trading.matcher.model;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class OrderBook {
    private final PriorityQueue<Order> buyQueue;
    private final PriorityQueue<Order> sellQueue;

    public ReentrantLock getLock() {
        return lock;
    }

    private final ReentrantLock lock = new ReentrantLock();

    public PriorityQueue<Order> getBuyQueue() {
        return buyQueue;
    }

    public PriorityQueue<Order> getSellQueue() {
        return sellQueue;
    }

    public OrderBook() {
        this.buyQueue = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice).reversed()
        );
        this.sellQueue = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice)
        );
    }
}
