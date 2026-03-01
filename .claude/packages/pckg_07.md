# PKG-07: Customer Service

**Status:** Not Started
**Depends on:** PKG-01, PKG-02
**Consumes (Kafka):** `dachshaus.auth.user-registered` from Auth Service (PKG-04)

## Goal

Customer profile management — no auth logic, profile created via Kafka event from Auth Service, CRUD for addresses and wishlists.

## Produces

- GraphQL subgraph: `me` query, profile updates, address management, wishlist
- `AuthEventConsumer` — creates customer profile when `user.registered` event arrives
- `CustomerEventProducer` — publishes profile changes to `customer.events`
- Federation entity: `Customer @key(fields: "id")`
- Flyway: `customers`, `addresses`, `wishlists` tables

## Interface Contracts

### GraphQL Subgraph (federation v2)

**Queries (authenticated):**
```graphql
me: Customer
```

**Mutations (authenticated):**
```graphql
updateProfile(input: UpdateProfileInput!): Customer!
addAddress(input: AddressInput!): Customer!
removeAddress(addressId: ID!): Customer!
addToWishlist(productId: ID!): Wishlist!
removeFromWishlist(productId: ID!): Wishlist!
```

**Federation entity:** `Customer @key(fields: "id")` — resolved by Order Service (adds `orders` field)

### Kafka (consumed)
- `dachshaus.auth.user-registered` → creates customer record
  - Idempotent: if customer with userId already exists, skip

### Kafka (produced)
- `dachshaus.customer.events` → `{ customerId, action, customer }` (key: customerId, cleanup: compact)
  - Consumed by: Streams (PKG-09) for order enrichment

### Database Notes
- `customers.id` is set FROM the auth event (not auto-generated)
- No `password_hash` column — credentials live in Auth DB only

## Acceptance Criteria

- [ ] `AuthEventConsumer` creates customer on `user.registered` event
- [ ] `AuthEventConsumer` is idempotent (duplicate events don't fail)
- [ ] `me` returns current user's profile based on `x-user-id`
- [ ] `addAddress` with `isDefault: true` unsets previous default
- [ ] `updateProfile` publishes customer event to Kafka
- [ ] Wishlist operations are idempotent (add twice = no error)
- [ ] Customer table has NO password-related columns

## Files to Create

```
services/customer/build.gradle.kts
services/customer/src/main/kotlin/com/dachshaus/customer/CustomerApplication.kt
services/customer/src/main/kotlin/com/dachshaus/customer/config/GraphQLConfig.kt
services/customer/src/main/kotlin/com/dachshaus/customer/config/DatabaseConfig.kt
services/customer/src/main/kotlin/com/dachshaus/customer/domain/model/Customer.kt
services/customer/src/main/kotlin/com/dachshaus/customer/domain/model/Address.kt
services/customer/src/main/kotlin/com/dachshaus/customer/domain/model/Wishlist.kt
services/customer/src/main/kotlin/com/dachshaus/customer/domain/repository/CustomerRepository.kt
services/customer/src/main/kotlin/com/dachshaus/customer/domain/service/CustomerService.kt
services/customer/src/main/kotlin/com/dachshaus/customer/graphql/resolver/CustomerResolver.kt
services/customer/src/main/kotlin/com/dachshaus/customer/graphql/resolver/CustomerFederationResolver.kt
services/customer/src/main/kotlin/com/dachshaus/customer/kafka/AuthEventConsumer.kt
services/customer/src/main/kotlin/com/dachshaus/customer/kafka/CustomerEventProducer.kt
services/customer/src/main/kotlin/com/dachshaus/customer/infrastructure/persistence/entity/CustomerEntity.kt
services/customer/src/main/kotlin/com/dachshaus/customer/infrastructure/persistence/entity/AddressEntity.kt
services/customer/src/main/kotlin/com/dachshaus/customer/infrastructure/persistence/mapper/CustomerMapper.kt
services/customer/src/main/resources/application.yml
services/customer/src/main/resources/application-local.yml
services/customer/src/main/resources/application-docker.yml
services/customer/src/main/resources/application-kubernetes.yml
services/customer/src/main/resources/db/migration/V1__create_customers.sql
services/customer/src/main/resources/db/migration/V2__create_addresses.sql
services/customer/src/main/resources/db/migration/V3__create_wishlists.sql
services/customer/src/main/resources/graphql/schema.graphqls
services/customer/src/test/kotlin/com/dachshaus/customer/domain/service/CustomerServiceTest.kt
services/customer/src/test/kotlin/com/dachshaus/customer/kafka/AuthEventConsumerTest.kt
```
