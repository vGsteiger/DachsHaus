# PKG-08: Cart Service

**Status:** Not Started
**Depends on:** PKG-01, PKG-02

## Goal

High-throughput Redis-backed shopping cart — extracted from Order Service for independent scalability, publishes cart events to Kafka.

## Produces

- GraphQL subgraph: cart queries and mutations
- `CartService` — Redis hash operations for O(1) add/update/remove per item
- `CartRedisRepository` — low-level Redis `HSET`, `HDEL`, `HGETALL`, `EXPIRE` operations
- `CartSnapshotService` — async periodic dump to PostgreSQL for analytics
- `CatalogClient` — validates products exist and are in stock before adding
- `CartEventProducer` — publishes `cart.updated` and `cart.checked-out`
- `ProductDataLoader` — batch product lookups for cart item resolution
- Federation entity: `Cart @key(fields: "userId")`

## Interface Contracts

### GraphQL Subgraph (federation v2)

**Queries (authenticated):**
```graphql
cart: Cart!
```

**Mutations (authenticated):**
```graphql
addToCart(input: AddToCartInput!): Cart!
updateCartItemQuantity(variantSku: String!, quantity: Int!): Cart!
removeFromCart(variantSku: String!): Cart!
clearCart: Cart!
```

**Federation:**
- `Cart @key(fields: "userId")`
- References `Product @key(fields: "id")` (external, resolved by Catalog)

### Internal RPC (consumed by Order Service during checkout)
```
checkoutAndClear(userId: String): Cart
  Atomically reads full cart and deletes from Redis
  Used by CheckoutOrchestrator in Order Service
```

### Redis Data Model
```
Key:    cart:{userId}     (Hash type)
Field:  item:{variantSku} → JSON { productId, sku, qty, priceCents, addedAt }
TTL:    30 days (reset on every write)
```

### Kafka (produced)
- `dachshaus.cart.updated` → `{ userId, action, variantSku, timestamp }` (key: userId)
- `dachshaus.cart.checked-out` → `{ userId, items, totalCents, timestamp }` (key: userId)
  - Consumed by: Order Service (PKG-06), Analytics

### Performance Targets
- `addToCart`: <5ms p99 (single Redis HSET)
- `getCart`: <10ms p99 (single Redis HGETALL)
- 100k+ ops/sec per Cart Service instance

## Acceptance Criteria

- [ ] `addToCart` validates product exists via `CatalogClient` before adding
- [ ] `addToCart` for same SKU updates quantity (not duplicates)
- [ ] `updateCartItemQuantity(qty: 0)` removes item
- [ ] Cart TTL resets to 30 days on every write operation
- [ ] `checkoutAndClear` atomically reads and deletes (no race condition)
- [ ] `clearCart` removes all items (verified: `HGETALL` returns empty)
- [ ] Redis hash operations: verified O(1) per item (no full-cart serialization)
- [ ] `CartSnapshotService` writes to PG without blocking Redis reads
- [ ] Integration test with Testcontainers Redis passes

## Files to Create

```
services/cart/build.gradle.kts
services/cart/src/main/kotlin/com/dachshaus/cart/CartApplication.kt
services/cart/src/main/kotlin/com/dachshaus/cart/config/RedisConfig.kt
services/cart/src/main/kotlin/com/dachshaus/cart/config/GraphQLConfig.kt
services/cart/src/main/kotlin/com/dachshaus/cart/config/KafkaConfig.kt
services/cart/src/main/kotlin/com/dachshaus/cart/domain/model/Cart.kt
services/cart/src/main/kotlin/com/dachshaus/cart/domain/model/CartItem.kt
services/cart/src/main/kotlin/com/dachshaus/cart/domain/service/CartService.kt
services/cart/src/main/kotlin/com/dachshaus/cart/domain/service/CartSnapshotService.kt
services/cart/src/main/kotlin/com/dachshaus/cart/domain/service/CatalogClient.kt
services/cart/src/main/kotlin/com/dachshaus/cart/graphql/resolver/CartResolver.kt
services/cart/src/main/kotlin/com/dachshaus/cart/graphql/resolver/CartFederationResolver.kt
services/cart/src/main/kotlin/com/dachshaus/cart/graphql/dataloader/ProductDataLoader.kt
services/cart/src/main/kotlin/com/dachshaus/cart/kafka/CartEventProducer.kt
services/cart/src/main/kotlin/com/dachshaus/cart/infrastructure/redis/CartRedisRepository.kt
services/cart/src/main/resources/application.yml
services/cart/src/main/resources/application-local.yml
services/cart/src/main/resources/application-docker.yml
services/cart/src/main/resources/application-kubernetes.yml
services/cart/src/main/resources/graphql/schema.graphqls
services/cart/src/test/kotlin/com/dachshaus/cart/domain/service/CartServiceTest.kt
services/cart/src/test/kotlin/com/dachshaus/cart/infrastructure/CartRedisIntegrationTest.kt
```
