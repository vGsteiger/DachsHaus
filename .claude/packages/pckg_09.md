# PKG-09: Kafka Streams Processors

**Status:** Not Started
**Depends on:** PKG-01, PKG-02
**Consumes (Kafka):** Events from Auth, Catalog, Order, Cart

## Goal

Event processing topologies — inventory reservation, order enrichment, notification routing. The "brain" of the async pipeline.

## Produces

- `InventoryTopology` — OrderPlaced → check stock → InventoryReserved/InventoryFailed
- `OrderEnrichmentTopology` — joins order + customer + product → enriched order view
- `NotificationTopology` — OrderConfirmed/Shipped → notification outbox
- Custom serdes: `OrderEventSerde`, `ProductEventSerde`, `EnrichedOrderSerde`
- `InventoryReservationProcessor` — state store lookup + stock deduction
- `OrderEnrichmentJoiner` — GlobalKTable joins on customerId and productId
- `InventoryStoreConfig` — persistent key-value state store for stock levels

## Interface Contracts

### InventoryTopology
```
Input:  dachshaus.order.events (filter: OrderPlaced only)
State:  inventory-store (populated from dachshaus.catalog.inventory, compacted)
Output: dachshaus.order.events → InventoryReserved or InventoryFailed

Logic:
  For each OrderItem in OrderPlaced:
    lookup variantSku in inventory-store
    if stock >= requested → deduct, accumulate reservations
    if stock < requested → emit InventoryFailed, abort
  If all items reserved → emit InventoryReserved
```

### OrderEnrichmentTopology
```
Input:   dachshaus.order.events (all types)
Tables:  dachshaus.customer.events (GlobalKTable, keyed by customerId)
         dachshaus.catalog.products (GlobalKTable, keyed by productId)
Output:  dachshaus.order.enriched → { order + full customer + full product details }

Logic:
  leftJoin order with customer on customerId
  For each OrderItem, leftJoin with product on productId
  Emit enriched order to output topic
```

### NotificationTopology
```
Input:  dachshaus.order.events (filter: OrderConfirmed, OrderShipped)
Output: dachshaus.notifications.outbox → { type, recipientEmail, templateId, data }

Logic:
  OrderConfirmed → { type: "order_confirmation", template: "order-confirmed" }
  OrderShipped   → { type: "shipping_update", template: "order-shipped", trackingNumber }
```

### Error Handling
- Deserialization failures → `DeadLetterPublisher` → `dachshaus.dlq`
- Processing failures (3 retries) → `DeadLetterPublisher` → `dachshaus.dlq`

### Kafka Streams Config
```
application.id: dachshaus-streams
Internal topics prefix: dachshaus-streams- (changelogs, repartition)
State store: persistent (RocksDB)
Exactly-once: processing.guarantee = exactly_once_v2
```

## Acceptance Criteria

- [ ] InventoryTopology correctly deducts stock and emits `InventoryReserved`
- [ ] InventoryTopology emits `InventoryFailed` when ANY item has insufficient stock
- [ ] InventoryTopology does NOT partially deduct (all-or-nothing reservation)
- [ ] OrderEnrichmentTopology produces enriched orders with full customer + product details
- [ ] NotificationTopology routes correct template per event type
- [ ] All topologies tested with `TopologyTestDriver` (no running Kafka needed)
- [ ] Failed messages land in DLQ with correct diagnostic headers
- [ ] State store survives restart (persistent RocksDB)

## Files to Create

```
services/streams/build.gradle.kts
services/streams/src/main/kotlin/com/dachshaus/streams/StreamsApplication.kt
services/streams/src/main/kotlin/com/dachshaus/streams/topology/InventoryTopology.kt
services/streams/src/main/kotlin/com/dachshaus/streams/topology/OrderEnrichmentTopology.kt
services/streams/src/main/kotlin/com/dachshaus/streams/topology/NotificationTopology.kt
services/streams/src/main/kotlin/com/dachshaus/streams/serde/OrderEventSerde.kt
services/streams/src/main/kotlin/com/dachshaus/streams/serde/ProductEventSerde.kt
services/streams/src/main/kotlin/com/dachshaus/streams/serde/EnrichedOrderSerde.kt
services/streams/src/main/kotlin/com/dachshaus/streams/processor/InventoryReservationProcessor.kt
services/streams/src/main/kotlin/com/dachshaus/streams/processor/OrderEnrichmentJoiner.kt
services/streams/src/main/kotlin/com/dachshaus/streams/store/InventoryStoreConfig.kt
services/streams/src/main/resources/application.yml
services/streams/src/main/resources/application-local.yml
services/streams/src/main/resources/application-docker.yml
services/streams/src/main/resources/application-kubernetes.yml
services/streams/src/test/kotlin/com/dachshaus/streams/topology/InventoryTopologyTest.kt
services/streams/src/test/kotlin/com/dachshaus/streams/topology/OrderEnrichmentTopologyTest.kt
```
