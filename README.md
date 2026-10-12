# OrderHub

An event-driven e-commerce order platform built from small Spring Boot services. Placing an order
returns immediately; stock reservation, payment and notification then happen in the background,
connected by Kafka events.

**Stack:** Java 17, Spring Boot 3, Spring Cloud (Eureka, Gateway), Apache Kafka, PostgreSQL, Redis,
Resilience4j, Docker Compose.

## How an order flows

```
 client
   |  POST /orders  (Idempotency-Key header optional)
   v
 api-gateway :8080 ---- Eureka (service discovery) :8761
   |
   v
 order-service :8081
   |  1. stock pre-check -> inventory-service (retry + circuit breaker)
   |  2. save order as PENDING (PostgreSQL), return 202 Accepted
   |  3. publish  order-created
   v
 ------------------------------ Kafka ------------------------------
   |
   |  order-created        -> inventory-service :8082  reserve stock
   |                            -> inventory-reserved   or  inventory-rejected
   |  inventory-reserved   -> payment-service :8083    charge (simulated)
   |                            -> payment-completed    or  payment-failed
   |  payment-completed    -> order-service      status CONFIRMED
   |                       -> notification-service :8084  "order confirmed"
   |  payment-failed       -> order-service      status CANCELLED
   |                       -> inventory-service  release the reserved stock
   |                       -> notification-service
   |  inventory-rejected   -> order-service      status CANCELLED
   |                       -> notification-service
```

Each service owns its own database (`orderdb`, `inventorydb`, `paymentdb`, `notificationdb`).

## Run it

You need Docker with about 4 GB of free memory.

```bash
docker compose up --build
```

The first build downloads dependencies and takes a few minutes. When everything has registered with
Eureka (open http://localhost:8761 to check), run the smoke test:

```bash
./scripts/smoke_test.sh
```

It places an order and waits until the order reaches `CONFIRMED`.

Or by hand:

```bash
curl -i -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"sku":"SKU-1","quantity":2,"amount":49.90}'

curl http://localhost:8080/orders/<id from the response>
curl http://localhost:8080/notifications/<id>
curl http://localhost:8080/inventory/SKU-1
```

Five SKUs (`SKU-1` to `SKU-5`) are seeded with 100,000 units each.

## Things worth trying

**Idempotency.** Send the same `Idempotency-Key` twice. The second call returns `200` with the same
order instead of creating a duplicate (the key is stored in Redis for 24 hours).

**Payment failure.** Orders above `payment.max-amount` (100000) are declined. Place an order with
`"amount": 150000` and watch it end up `CANCELLED` and the stock released.
`payment.failure-rate` (0.0 to 1.0) injects random declines.

**Inventory outage.** This is the resilience demo:

```bash
docker compose stop inventory-service
./scripts/load_test.py --requests 200 --concurrency 10   # orders are still accepted
docker compose start inventory-service                    # pending orders then complete
```

While inventory-service is down, the stock pre-check fails, is retried with exponential backoff, and
the circuit breaker opens (`curl localhost:8081/actuator/health` shows its state). The fallback
accepts the order, and inventory-service reserves stock from the Kafka backlog when it comes back.

## Running the tests

```bash
mvn verify
```

The unit tests cover stock reservation (including duplicate delivery and release) and order
placement. They use Mockito and need no running infrastructure.

## Measuring performance

`scripts/load_test.py` posts orders concurrently and prints average, p50, p95 and p99 latency, plus
throughput. The latency is the time to the `202 Accepted`, which is what a customer waits for.

```bash
python3 scripts/load_test.py --requests 2000 --concurrency 50
```

Record your own results here, with the machine you ran them on:

| Date | Machine | Requests | Concurrency | p50 | p95 | p99 | Throughput |
|------|---------|----------|-------------|-----|-----|-----|------------|
|      |         |          |             |     |     |     |            |

## Design notes and known limits

- **Choreography, not orchestration.** Each service reacts to events and publishes its own. It is
  simple and loosely coupled, but the overall flow is only visible by reading all the listeners.
- **Optimistic locking for stock.** `StockItem` has a `@Version` column. If two orders race for the
  same SKU, one write is rejected and retried (up to 5 times) rather than overselling.
- **Idempotent consumers.** Inventory and payment record what they processed per order id, so a
  duplicate Kafka delivery does not reserve or charge twice.
- **Dual write in order-service.** The order is saved and then the event is published as two
  separate steps. If the process dies in between, the order stays `PENDING` with no event. The
  standard fix is a transactional outbox table; it is not implemented here.
- **No dead-letter topic.** A message that keeps failing is retried by Spring Kafka's default error
  handler and then skipped.
- **Payment is simulated.** There is no real payment provider.
- **Schema.** Tables are created by Hibernate (`ddl-auto: update`). A real deployment would use
  Flyway or Liquibase migrations.
