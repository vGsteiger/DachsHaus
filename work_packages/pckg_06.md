# PKG-06: Order Service

**Status:** Not Started
**Depends on:** PKG-01, PKG-02
**Consumes (runtime):** Cart Service (PKG-08) for `checkoutAndClear`

## Goal

Order lifecycle management — checkout orchestration (calls Cart Service), order creation, status tracking, GraphQL subscriptions for live updates.

## Produces

- GraphQL subgraph: orders, checkout mutation, order status subscription
- `CheckoutOrchestrator` — calls Cart Service → creates order → publishes `OrderPlaced`
- `OrderStatusSubscription` — WebSocket-based live order status updates
- Sealed class event hierarchy: `OrderPlaced`, `OrderConfirmed`, `OrderShipped`, `OrderCancelled`
- `InventoryResponseConsumer` — listens for inventory reservation results from Streams
- Federation: extends `Customer` with `orders` field
- Flyway: `orders`, `order_items` tables

## Interface Contracts

### GraphQL Subgraph (federation v2)

**Queries (authenticated):**
```graphql
order(id: ID!): Order
myOrders(page: PageInput): OrderConnection!
```

**Mutations (authenticated):**
```graphql
checkout(input: CheckoutInput!): CheckoutResult!
cancelOrder(orderId: ID!, reason: String): Order!
```

**Subscriptions (authenticated):**
```graphql
orderStatusChanged(orderId: ID!): OrderStatusUpdate!
```

**Federation:**
- `Order @key(fields: "id")`
- Extends `Customer @key(fields: "id")` with `orders: [Order!]!`
- References `Product @key(fields: "id")` (external)

### Kafka (produced)
- `dachshaus.order.events` → `OrderEvent` sealed class (key: orderId)
  - Types: `OrderPlaced`, `OrderConfirmed`, `OrderShipped`, `OrderCancelled`
  - Consumed by: Streams (PKG-09)

### Kafka (consumed)
- `dachshaus.order.events` ← `InventoryReserved` / `InventoryFailed` (from Streams PKG-09)
  - Triggers: PENDING → CONFIRMED or PENDING → CANCELLED

### Checkout Flow
1. `cartClient.checkoutAndClear(userId)` → Cart snapshot
2. Validate cart not empty
3. Create Order (PENDING) in orders DB
4. Publish `OrderPlaced` to Kafka
5. Return `CheckoutResult { order, errors? }`

### Order Status State Machine
```
PENDING → CONFIRMED → PAID → FULFILLING → SHIPPED → DELIVERED
PENDING → CANCELLED (inventory failed)
CONFIRMED → CANCELLED (customer cancel)
Any → REFUNDED (admin)
```

## Acceptance Criteria

- [ ] `checkout` atomically gets cart, creates order, publishes event
- [ ] `checkout` with empty cart returns `CheckoutError(CART_EMPTY)`
- [ ] `orderStatusChanged` subscription delivers real-time updates over WebSocket
- [ ] `InventoryResponseConsumer` transitions order to CONFIRMED or CANCELLED
- [ ] `myOrders` returns only orders for the authenticated user
- [ ] Sealed class event hierarchy is exhaustive in `when` expressions
- [ ] Order status transitions are validated (no invalid transitions)

## Files to Create

```
services/order/build.gradle.kts
services/order/src/main/kotlin/com/dachshaus/order/OrderApplication.kt
services/order/src/main/kotlin/com/dachshaus/order/config/GraphQLConfig.kt
services/order/src/main/kotlin/com/dachshaus/order/config/KafkaConfig.kt
services/order/src/main/kotlin/com/dachshaus/order/config/WebSocketConfig.kt
services/order/src/main/kotlin/com/dachshaus/order/config/DatabaseConfig.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/model/Order.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/model/OrderItem.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/model/OrderStatus.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/repository/OrderRepository.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/service/OrderService.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/service/CheckoutOrchestrator.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/event/OrderEvent.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/event/OrderPlaced.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/event/OrderConfirmed.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/event/OrderShipped.kt
services/order/src/main/kotlin/com/dachshaus/order/domain/event/OrderCancelled.kt
services/order/src/main/kotlin/com/dachshaus/order/graphql/resolver/OrderResolver.kt
services/order/src/main/kotlin/com/dachshaus/order/graphql/resolver/OrderFederationResolver.kt
services/order/src/main/kotlin/com/dachshaus/order/graphql/subscription/OrderStatusSubscription.kt
services/order/src/main/kotlin/com/dachshaus/order/kafka/OrderEventProducer.kt
services/order/src/main/kotlin/com/dachshaus/order/kafka/InventoryResponseConsumer.kt
services/order/src/main/kotlin/com/dachshaus/order/infrastructure/persistence/entity/OrderEntity.kt
services/order/src/main/kotlin/com/dachshaus/order/infrastructure/persistence/entity/OrderItemEntity.kt
services/order/src/main/kotlin/com/dachshaus/order/infrastructure/persistence/mapper/OrderMapper.kt
services/order/src/main/resources/application.yml
services/order/src/main/resources/application-local.yml
services/order/src/main/resources/application-docker.yml
services/order/src/main/resources/application-kubernetes.yml
services/order/src/main/resources/db/migration/V1__create_orders.sql
services/order/src/main/resources/db/migration/V2__create_order_items.sql
services/order/src/main/resources/graphql/schema.graphqls
services/order/src/test/kotlin/com/dachshaus/order/domain/service/OrderServiceTest.kt
services/order/src/test/kotlin/com/dachshaus/order/domain/service/CheckoutOrchestratorTest.kt
services/order/src/test/kotlin/com/dachshaus/order/kafka/OrderEventIntegrationTest.kt
```
