package com.trading.matcher;

import com.trading.matcher.grpc.MatcherServiceGrpc;
import com.trading.matcher.grpc.OrderSide;
import com.trading.matcher.grpc.PlaceOrderRequest;
import com.trading.matcher.grpc.PlaceOrderResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatcherLatencyTest {

    private static ManagedChannel channel;
    private static MatcherServiceGrpc.MatcherServiceBlockingStub stub;

    @BeforeAll
    static void setup() {
        channel = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        stub = MatcherServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void tearDown() {
        channel.shutdown();
    }

    @Test
    void measurePlaceOrderLatency() {

        int numberOfRequests = 1_000;

        List<Long> latencies = new ArrayList<>();

        // Warm-up
        for (int i = 0; i < 100; i++) {
            sendOrder(i);
        }

        // Actual measurement
        for (int i = 0; i < numberOfRequests; i++) {

            long start = System.nanoTime();

            PlaceOrderResponse response = sendOrder(1_000 + i);

            long end = System.nanoTime();

            assertTrue(response.getAccepted());

            long latencyNanos = end - start;
            latencies.add(latencyNanos);
        }

        printStatistics(latencies);
    }

    private static PlaceOrderResponse sendOrder(long orderId) {

        PlaceOrderRequest request = PlaceOrderRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(1)
                .setSide(OrderSide.BUY)
                .setBase("BTC")
                .setQuote("USDT")
                .setQuantity("0.001")
                .setPrice("600")
                .build();

        return stub.placeOrder(request);
    }

    private static void printStatistics(List<Long> latencies) {

        Collections.sort(latencies);

        double averageMicros = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000.0;

        double minMicros = latencies.getFirst() / 1_000.0;
        double maxMicros = latencies.getLast() / 1_000.0;

        double p50 = percentile(latencies, 0.50) / 1_000.0;
        double p95 = percentile(latencies, 0.95) / 1_000.0;
        double p99 = percentile(latencies, 0.99) / 1_000.0;

        System.out.println();
        System.out.println("===== Matcher Latency =====");
        System.out.printf("Requests: %d%n", latencies.size());
        System.out.printf("Min:      %.2f µs%n", minMicros);
        System.out.printf("Average:  %.2f µs%n", averageMicros);
        System.out.printf("p50:      %.2f µs%n", p50);
        System.out.printf("p95:      %.2f µs%n", p95);
        System.out.printf("p99:      %.2f µs%n", p99);
        System.out.printf("Max:      %.2f µs%n", maxMicros);
        System.out.println("============================");
    }

    private static long percentile(List<Long> sortedValues, double percentile) {

        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;

        return sortedValues.get(Math.max(index, 0));
    }
}