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

class MatcherThroughputTest {

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
    void measureThroughput() throws Exception {

        int totalRequests = 100_000;
        int concurrency = 100;

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrency);

        CountDownLatch ready =
                new CountDownLatch(concurrency);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Long> latencies =
                Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();

        int requestsPerThread =
                totalRequests / concurrency;

        for (int thread = 0; thread < concurrency; thread++) {

            int threadId = thread;

            futures.add(executor.submit(() -> {

                ready.countDown();

                try {
                    start.await();

                    for (int i = 0; i < requestsPerThread; i++) {

                        long orderId =
                                1_000_000L
                                        + threadId * requestsPerThread
                                        + i;

                        PlaceOrderRequest request =
                                PlaceOrderRequest.newBuilder()
                                        .setOrderId(orderId)
                                        .setUserId(threadId + 1)
                                        .setSide(OrderSide.BUY)
                                        .setBase("BTC")
                                        .setQuote("USDT")
                                        .setQuantity("0.001")
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

        // Wait until all workers are ready.
        ready.await();

        long startTime = System.nanoTime();

        // Release all workers simultaneously.
        start.countDown();

        // Wait for all workers to finish.
        for (Future<?> future : futures) {
            future.get();
        }

        long endTime = System.nanoTime();

        executor.shutdown();

        double totalSeconds =
                (endTime - startTime) / 1_000_000_000.0;

        double throughput =
                totalRequests / totalSeconds;

        System.out.println();
        System.out.println("====================================");
        System.out.println("Matcher Throughput");
        System.out.println("====================================");
        System.out.printf(
                "Requests:      %d%n",
                totalRequests
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
                "Throughput:    %.2f orders/sec%n",
                throughput
        );

        printLatencyStatistics(latencies);

        System.out.println("====================================");
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
}