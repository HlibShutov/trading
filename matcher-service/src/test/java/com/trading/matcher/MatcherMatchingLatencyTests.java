package com.trading.matcher;

import com.trading.matcher.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatcherMatchingLatencyTest {
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
    void measure10000Matches() {

        int depth = 10_000;

        populateSellBook(depth);

        BigDecimal quantity = new BigDecimal("10.000");

        // Warmup
        sendBuyOrder(9_000_000L, quantity);

        // IMPORTANT:
        // warmup consumed the book, so populate again
        populateSellBook(depth);

        long start = System.nanoTime();

        PlaceOrderResponse response =
                sendBuyOrder(9_000_001L, quantity);

        long end = System.nanoTime();

        assertTrue(response.getAccepted());

        double latencyMs =
                (end - start) / 1_000_000.0;

        System.out.printf(
                "10,000 matches latency: %.2f ms%n",
                latencyMs
        );
    }
    @Test
    void measureDeepBookMatchingLatency() {

        int[] depths = {10, 100, 1_000, 10_000};

        for (int depth : depths) {

            System.out.println();
            System.out.println("====================================");
            System.out.println("Testing depth: " + depth);
            System.out.println("====================================");

            measureDepth(depth);
        }
    }

    private static void measureDepth(int depth) {

        BigDecimal quantity = BigDecimal.valueOf(depth)
                .multiply(new BigDecimal("0.001"));

        /*
         * Start from a clean order book.
         */
        resetMatcher();

        /*
         * Populate the book once for warmup.
         */
        populateSellBook(depth);

        /*
         * Warmup request.
         * This is NOT included in the measurement.
         */
        sendBuyOrder(
                10_000_000L + depth,
                quantity
        );

        /*
         * Clear everything created by warmup.
         */
        resetMatcher();

        /*
         * Create the exact order book that we want
         * to measure.
         */
        populateSellBook(depth);

        List<Long> latencies = new ArrayList<>();

        int requests = 10;

        for (int i = 0; i < requests; i++) {

            /*
             * Every measurement starts with exactly
             * 'depth' resting SELL orders.
             */
            resetMatcher();

            populateSellBook(depth);

            long start = System.nanoTime();

            PlaceOrderResponse response = sendBuyOrder(
                    20_000_000L + depth * 100L + i,
                    quantity
            );

            long end = System.nanoTime();

            assertTrue(response.getAccepted());

            latencies.add(end - start);
        }

        printStatistics(latencies);
    }

    private static void resetMatcher() {

        ResetResponse response = stub.reset(
                ResetRequest.newBuilder().build()
        );

        assertTrue(response.getSuccess());
    }

    private static void populateSellBook(int depth) {

        for (int i = 0; i < depth; i++) {

            PlaceOrderResponse response = sendSellOrder(
                    1_000_000L + i,
                    "0.001",
                    "100"
            );

            assertTrue(response.getAccepted());
        }
    }

    private static PlaceOrderResponse sendSellOrder(
            long orderId,
            String quantity,
            String price) {

        PlaceOrderRequest request = PlaceOrderRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(2)
                .setSide(OrderSide.SELL)
                .setBase("BTC")
                .setQuote("USDT")
                .setQuantity(quantity)
                .setPrice(price)
                .build();

        return stub.placeOrder(request);
    }

    private static PlaceOrderResponse sendBuyOrder(
            long orderId,
            BigDecimal quantity) {

        PlaceOrderRequest request = PlaceOrderRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(1)
                .setSide(OrderSide.BUY)
                .setBase("BTC")
                .setQuote("USDT")
                .setQuantity(quantity.toString())
                .setPrice("100")
                .build();

        return stub.placeOrder(request);
    }

    private static void printStatistics(List<Long> latencies) {

        Collections.sort(latencies);

        double average = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000_000.0;

        double min = latencies.getFirst() / 1_000_000.0;
        double max = latencies.getLast() / 1_000_000.0;

        double p50 = percentile(latencies, 0.50) / 1_000_000.0;
        double p95 = percentile(latencies, 0.95) / 1_000_000.0;
        double p99 = percentile(latencies, 0.99) / 1_000_000.0;

        System.out.printf("Min:      %.2f ms%n", min);
        System.out.printf("Average:  %.2f ms%n", average);
        System.out.printf("p50:      %.2f ms%n", p50);
        System.out.printf("p95:      %.2f ms%n", p95);
        System.out.printf("p99:      %.2f ms%n", p99);
        System.out.printf("Max:      %.2f ms%n", max);
    }

    private static long percentile(
            List<Long> sortedValues,
            double percentile) {

        int index = (int) Math.ceil(
                percentile * sortedValues.size()
        ) - 1;

        return sortedValues.get(
                Math.max(index, 0)
        );
    }
}
