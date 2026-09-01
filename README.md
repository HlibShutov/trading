# Trading System

A distributed trading system built with Java, Spring Boot, gRPC, Apache Kafka, PostgreSQL, and Docker.

The project implements a basic exchange-style workflow where users can place BUY and SELL orders, funds/assets are reserved in their wallets, orders are matched by a dedicated matching engine, and executed trades are propagated asynchronously through Kafka.

## Architecture

The system consists of three main services:

* Order Service — accepts and manages user orders.
* Matcher Service — maintains order books and matches BUY and SELL orders.
* Wallet Service — manages user balances and reservations and processes executed trades.

Supporting infrastructure:

* PostgreSQL — persistent storage for orders and wallet balances.
* Apache Kafka — asynchronous communication for executed trades.
* gRPC — synchronous communication between Order Service and Matcher Service.
* Docker Compose — runs the complete system locally.

### High-level architecture

```text
                    ┌─────────────────┐
                    │     Client      │
                    └────────┬────────┘
                             │
                             │ REST
                             ▼
                    ┌─────────────────┐
                    │  Order Service  │
                    └────────┬────────┘
                             │
                             │ gRPC
                             ▼
                    ┌─────────────────┐
                    │ Matcher Service │
                    │                 │
                    │   Order Books   │
                    └────────┬────────┘
                             │
                             │ Kafka
                             │ TradeExecutedEvent
                             ▼
                    ┌─────────────────┐
                    │  Wallet Service │
                    └─────────────────┘

       ┌──────────────────┐       ┌──────────────────┐
       │  Order PostgreSQL │       │ Wallet PostgreSQL│
       └──────────────────┘       └──────────────────┘
```

## Order Flow

### 1. Place an order

A client sends an order to the Order Service.

Example:

```http
POST /order
Content-Type: application/json
```

```json
{
  "userId": 1,
  "base": "BTC",
  "quote": "USDT",
  "quantity": 0.001,
  "price": 600,
  "side": "BUY"
}
```

The Order Service:

1. Validates the order.
2. Reserves the required wallet funds.
3. Stores the order in PostgreSQL.
4. Sends the order to the Matcher Service using gRPC.

### 2. Match the order

The Matcher Service maintains the order books and attempts to match incoming orders against existing orders.

For example:

```text
BUY  BTC  0.001 @ 600
        │
        ▼
SELL BTC  0.001 @ 600
        │
        ▼
       MATCH
```

When a match occurs, the matcher creates a `TradeExecutedEvent`.

### 3. Publish the trade

The Matcher Service publishes the event to Kafka:

```text
trade-executed
```

The event contains the information required by downstream services:

```java
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
```

### 4. Process the trade

The Wallet Service consumes the Kafka event.

For a BUY order:

```text
Buyer:
    + base asset
    - reserved quote asset
```

For a SELL order:

```text
Seller:
    + quote asset
    - reserved base asset
```

The wallet update is performed transactionally.

## Communication

The project intentionally uses two different communication mechanisms.

### gRPC

gRPC is used for synchronous Order Service → Matcher Service communication.

```text
Order Service
      │
      │ PlaceOrder()
      ▼
Matcher Service
```

This allows the Order Service to immediately know whether the matcher accepted the order.

### Kafka

Kafka is used for asynchronous trade events.

```text
Matcher Service
      │
      │ TradeExecutedEvent
      ▼
   Kafka
      │
      ▼
Wallet Service
```

This decouples trade execution from wallet processing.

The Wallet Service and Order Service use separate Kafka consumer groups so that both services independently receive the same trade event.

## Running the Project

### Start the system

From the project root:

```bash
docker compose up --build
```

This starts:

* Order Service
* Matcher Service
* Wallet Service
* Order PostgreSQL
* Wallet PostgreSQL
* Kafka

## Testing

The project contains several types of tests.

### Unit Tests

Unit tests verify the business logic of individual components independently of the complete distributed system.

Examples include:

* order validation
* order matching
* wallet balance calculations
* order status changes
* BUY/SELL matching behaviour

### Performance Tests

The matching engine was also tested using gRPC clients with concurrent requests.

The performance tests measure:

* request latency
* matching latency
* throughput
* behaviour with increasingly deep order books
* concurrent matching throughput

The tests are intended as benchmarks rather than strict production capacity limits.

## Performance Results

Tests were executed against the locally running Matcher Service.

### Basic Matching Latency

The matcher was tested with individual gRPC requests.

One measured run produced:

```text
Requests: 1000

Min:      452.62 µs
Average:  832.42 µs
p50:      759.65 µs
p95:      1396.02 µs
p99:      2144.53 µs
Max:      3726.99 µs
```

This measures the end-to-end latency of submitting an order through gRPC and processing it in the matcher.

### Deep Order Book Matching Latency

The matcher was tested with increasingly deep order books.

| Order book depth |      Min |    Average |      p50 |        p95 |        Max |
| ---------------: | -------: | ---------: | -------: | ---------: | ---------: |
|               10 |  1.41 ms |    1.83 ms |  1.70 ms |    2.45 ms |    2.45 ms |
|              100 |  1.35 ms |    2.13 ms |  1.87 ms |    3.59 ms |    3.59 ms |
|            1,000 |  2.49 ms |    6.02 ms |  5.54 ms |   10.12 ms |   10.12 ms |

The results show that matching latency generally increases as the order book becomes deeper.

### 10,000 Matches

A separate benchmark measured a workload containing 10,000 matches:

```text
10,000 matches latency: 205.04 ms
```
### Concurrent Order Throughput

The matcher was tested with:

```text
Requests:      100,000
Concurrency:   100
```

Results:

```text
Total time:    5792.73 ms
Throughput:    17263.02 orders/sec

Min latency:   0.18 ms
Avg latency:   5.73 ms
p50 latency:   2.48 ms
p95 latency:   19.99 ms
p99 latency:   37.34 ms
Max latency:   568.21 ms
```

This test measures the system's ability to process a large number of concurrent order requests.

### Concurrent Matching Throughput

A separate benchmark populated the order book with 100,000 SELL orders and then submitted 100,000 BUY orders concurrently.

Configuration:

```text
Orders:        100,000
Concurrency:   100
```

Each BUY order was designed to match a corresponding SELL order.

Results:

```text
Orders:        100,000
Matches:       100,000
Concurrency:   100

Total time:    3886.79 ms
Orders/sec:    25728.18
Matches/sec:   25728.18

Min latency:   0.21 ms
Avg latency:   3.87 ms
p50 latency:   2.53 ms
p95 latency:   13.53 ms
p99 latency:   25.24 ms
Max latency:   55.23 ms
```

The observed throughput was therefore approximately:

**25,700 matching orders per second** under this benchmark configuration.

The relatively low maximum latency compared with the earlier general throughput test is also notable:

```text
Matching throughput max: 55.23 ms
General throughput max:  568.21 ms
```

## Design Goals

The project focuses on demonstrating several concepts commonly used in distributed financial systems:

* Microservice architecture
* Synchronous and asynchronous service communication
* Order-book based matching
* Transactional wallet updates
* Event-driven architecture
* Kafka consumer groups
* gRPC APIs
* PostgreSQL persistence
* Containerized development
* Performance benchmarking
* Concurrent request processing
