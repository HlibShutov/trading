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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatcherMatchingThroughputTest {

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
    void measureMatchingThroughput() throws Exception {

        int totalOrders = 100_000;
        int concurrency = 100;

        BigDecimalHolder quantity = new BigDecimalHolder("0.001");

        /*
         * Populate the sell book BEFORE measurement.
         *
         * Every BUY below will match exactly one SELL.
         */
        populateSellBook(totalOrders);

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrency);

        CountDownLatch ready =
                new CountDownLatch(concurrency);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Long> latencies =
                Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();

        int ordersPerThread =
                totalOrders / concurrency;

        for (int thread = 0; thread < concurrency; thread++) {

            int threadId = thread;

            futures.add(executor.submit(() -> {

                ready.countDown();

                try {
                    start.await();

                    for (int i = 0; i < ordersPerThread; i++) {

                        long orderId =
                                2_000_000L
                                        + threadId * ordersPerThread
                                        + i;

                        PlaceOrderRequest request =
                                PlaceOrderRequest.newBuilder()
                                        .setOrderId(orderId)
                                        .setUserId(
                                                10_000L + threadId
                                        )
                                        .setSide(OrderSide.BUY)
                                        .setBase("BTC")
                                        .setQuote("USDT")
                                        .setQuantity(quantity.value)
                                        .setPrice("100")
                                        .build();

                        long requestStart =
                                System.nanoTime();

                        PlaceOrderResponse response =
                                stub.placeOrder(request);

                        long requestEnd =
                                System.nanoTime();

                        assertTrue(response.getAccepted());

                        latencies.add(
                                requestEnd - requestStart
                        );
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
        }

        /*
         * Make sure all threads are ready before starting.
         */
        ready.await();

        long startTime = System.nanoTime();

        /*
         * Start all clients simultaneously.
         */
        start.countDown();

        /*
         * Wait for all requests to finish.
         */
        for (Future<?> future : futures) {
            future.get();
        }

        long endTime = System.nanoTime();

        executor.shutdown();

        double totalSeconds =
                (endTime - startTime) / 1_000_000_000.0;

        double throughput =
                totalOrders / totalSeconds;

        System.out.println();
        System.out.println("====================================");
        System.out.println("Matcher Matching Throughput");
        System.out.println("====================================");

        System.out.printf(
                "Orders:        %,d%n",
                totalOrders
        );

        System.out.printf(
                "Matches:       %,d%n",
                totalOrders
        );

        System.out.printf(
                "Concurrency:   %d%n",
                concurrency
        );

        System.out.printf(
                "Total time:    %.2f ms%n",
                totalSeconds * 1000
        );

        System.out.printf(
                "Orders/sec:    %.2f%n",
                throughput
        );

        System.out.printf(
                "Matches/sec:   %.2f%n",
                throughput
        );

        printLatencyStatistics(latencies);

        System.out.println("====================================");
    }

    private static void populateSellBook(int count) {

        System.out.println(
                "Populating sell book with "
                        + count
                        + " orders..."
        );

        for (int i = 0; i < count; i++) {

            PlaceOrderRequest request =
                    PlaceOrderRequest.newBuilder()
                            .setOrderId(
                                    1_000_000L + i
                            )
                            .setUserId(
                                    100_000L + i
                            )
                            .setSide(OrderSide.SELL)
                            .setBase("BTC")
                            .setQuote("USDT")
                            .setQuantity("0.001")
                            .setPrice("100")
                            .build();

            PlaceOrderResponse response =
                    stub.placeOrder(request);

            assertTrue(response.getAccepted());
        }

        System.out.println("Sell book populated.");
    }

    private static void printLatencyStatistics(
            List<Long> latencies) {

        Collections.sort(latencies);

        double average =
                latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0)
                        / 1_000_000.0;

        double min =
                latencies.getFirst()
                        / 1_000_000.0;

        double max =
                latencies.getLast()
                        / 1_000_000.0;

        double p50 =
                percentile(latencies, 0.50)
                        / 1_000_000.0;

        double p95 =
                percentile(latencies, 0.95)
                        / 1_000_000.0;

        double p99 =
                percentile(latencies, 0.99)
                        / 1_000_000.0;

        System.out.printf(
                "Min latency:  %.2f ms%n",
                min
        );

        System.out.printf(
                "Avg latency:  %.2f ms%n",
                average
        );

        System.out.printf(
                "p50 latency:  %.2f ms%n",
                p50
        );

        System.out.printf(
                "p95 latency:  %.2f ms%n",
                p95
        );

        System.out.printf(
                "p99 latency:  %.2f ms%n",
                p99
        );

        System.out.printf(
                "Max latency:  %.2f ms%n",
                max
        );
    }

    private static long percentile(
            List<Long> sortedValues,
            double percentile) {

        int index =
                (int) Math.ceil(
                        percentile * sortedValues.size()
                ) - 1;

        return sortedValues.get(
                Math.max(index, 0)
        );
    }

    /*
     * Tiny holder just to keep the quantity constant
     * without repeatedly constructing BigDecimal.
     */
    private static class BigDecimalHolder {
        private final String value;

        private BigDecimalHolder(String value) {
            this.value = value;
        }
    }
}
