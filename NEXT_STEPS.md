# DachsHaus - Next Steps (Prioritized Action Plan)

**Last Updated**: 2026-03-17
**Based on**: Actual codebase analysis (see `IMPLEMENTATION_STATUS.md`)

This document provides a concrete, prioritized action plan based on the **actual current state** of the codebase.

---

## Immediate Priority: Unblock Critical Path

The most urgent work is to **complete PKG-04 (Auth Service)**, which blocks all other services. No user operations can work without authentication.

---

## Week 1: Complete Common Module & Start Auth Service

### Day 1-2: Finish PKG-02 Common Module (15% remaining)

**Goal**: Unblock all Kotlin services

#### Tasks:
1. **Implement HMAC verification in GatewaySignatureFilter**
   - File: `services/common/src/main/kotlin/com/dachshaus/common/security/GatewaySignatureFilter.kt`
   - Currently: Skeleton structure, no verification logic
   - Add: Parse `X-Gateway-Signature` header, verify HMAC-SHA256, check timestamp skew (±30s)
   - Reject: 403 for missing/invalid/expired signatures

2. **Validate SignatureVerifier constant-time comparison**
   - File: `services/common/src/main/kotlin/com/dachshaus/common/security/SignatureVerifier.kt`
   - Ensure: No early exit in comparison (prevents timing attacks)
   - Use: `MessageDigest.isEqual()` or equivalent

3. **Implement DeadLetterPublisher**
   - File: `services/common/src/main/kotlin/com/dachshaus/common/kafka/DeadLetterPublisher.kt`
   - Currently: Skeleton only
   - Add: 7 diagnostic headers (source.topic, source.partition, source.offset, source.service, error.class, error.message, timestamp)

4. **Add missing topic names to TopicNames**
   - File: `services/common/src/main/kotlin/com/dachshaus/common/kafka/TopicNames.kt`
   - Currently: 6/11 topics defined
   - Add: `dachshaus.auth.revocations`, `dachshaus.catalog.products`, `dachshaus.catalog.inventory`, `dachshaus.order.enriched`, `dachshaus.notifications.outbox`

5. **Write tests**
   - `GatewaySignatureFilterTest.kt`: Integration test (valid/invalid/expired signatures)
   - `SignatureVerifierTest.kt`: Unit test (constant-time comparison)

**Acceptance**: All PKG-02 acceptance criteria pass, tests green

---

### Day 3-5: Start PKG-04 Auth Service (Core JWT Infrastructure)

**Goal**: Get JWT token issuance and verification working

#### Tasks:
1. **Generate RSA key pair**
   ```bash
   cd services/auth/src/main/resources/keys
   openssl genrsa -out auth-private.pem 2048
   openssl rsa -in auth-private.pem -pubout -out auth-public.pem
   ```

2. **Implement JwtConfig**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/config/JwtConfig.kt`
   - Load RSA keys from resources
   - Define TTLs: 15min access, 7day refresh

3. **Implement TokenService**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/TokenService.kt`
   - Functions:
     - `generateAccessToken(userId, email, roles): String` — RS256, 15min TTL
     - `generateRefreshToken(userId): String` — RS256, 7day TTL
     - `verifyToken(token): TokenClaims?` — verify signature + expiration
     - `generateJwks(): JwksResponse` — export RSA public key in JWK format

4. **Update JwksController**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/api/rest/JwksController.kt`
   - Currently: Returns empty `{keys: []}`
   - Change: Call `tokenService.generateJwks()`

5. **Write tests**
   - `TokenServiceTest.kt`: Unit tests for JWT generation, verification, expiration

**Acceptance**: Can generate and verify JWTs, JWKS endpoint returns valid RSA public key

---

## Week 2: Complete Auth Service Business Logic

### Day 6-7: Implement Auth Registration & Login

#### Tasks:
1. **Implement CredentialRepository**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/repository/CredentialRepository.kt`
   - JpaRepository interface: `findByEmail()`, `save()`

2. **Implement RefreshTokenRepository**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/repository/RefreshTokenRepository.kt`
   - JpaRepository interface: `findByToken()`, `findByUserId()`, `deleteByUserId()`

3. **Implement Entity classes**
   - `CredentialEntity.kt`: JPA entity matching V1 migration
   - `RefreshTokenEntity.kt`: JPA entity matching V2 migration

4. **Implement AuthService.register()**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/AuthService.kt`
   - Hash password with BCrypt cost=12
   - Save to database
   - Publish `user.registered` event to Kafka

5. **Implement AuthService.login()**
   - Verify password (BCrypt compare)
   - Generate access + refresh tokens
   - Store refresh token in database (hashed)
   - Return `AuthPayload { accessToken, refreshToken }`

6. **Implement LoginThrottleService**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/LoginThrottleService.kt`
   - In-memory tracking: Map<email, List<timestamp>>
   - 5 attempts per 15 minutes → 429
   - 10 consecutive failures → 30min lockout

7. **Implement AuthEventProducer**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/kafka/AuthEventProducer.kt`
   - Publish to `dachshaus.auth.user-registered`

8. **Update AuthResolver.login()**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/api/graphql/resolver/AuthResolver.kt`
   - Currently: Returns empty token
   - Change: Call `authService.login()`

9. **Implement AuthResolver.register()**
   - Add mutation: Call `authService.register()`

**Acceptance**: Register → Login flow works, JWT tokens issued

---

### Day 8-10: Implement Token Refresh, Revocation, and /auth/verify

#### Tasks:
1. **Implement RevocationService**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/RevocationService.kt`
   - In-memory cache: `ConcurrentHashMap<userId, isRevoked>`
   - On startup: Load from `revocations` table
   - Kafka consumer: Listen to `dachshaus.auth.revocations`, update cache

2. **Implement AuthService.refreshToken()**
   - Verify refresh token (JWT verify + not revoked + exists in DB)
   - Revoke old refresh token
   - Generate new access + refresh pair
   - Return new tokens

3. **Implement AuthService.logout()**
   - Revoke all refresh tokens for user
   - Publish `revocations` event to Kafka

4. **Implement AuthResolver mutations**
   - `refreshToken(token: String!): TokenPair!`
   - `logout: Boolean!`
   - `authStatus: AuthStatus!`

5. **Implement VerifyController.verify()**
   - File: `services/auth/src/main/kotlin/com/dachshaus/auth/api/rest/VerifyController.kt`
   - Currently: Returns `{status: "ok"}`
   - Change:
     - Call `tokenService.verifyToken(token)`
     - Check `revocationService.isRevoked(userId)`
     - Return `{ valid: true/false, userId, email, roles, reason }`
     - Target: <5ms latency

6. **Write comprehensive tests**
   - `AuthServiceTest.kt`: Register, login, refresh, logout, throttling
   - `RevocationServiceTest.kt`: Cache warming, Kafka sync
   - `LoginThrottleServiceTest.kt`: Rate limits, lockouts
   - `VerifyControllerIntegrationTest.kt`: Full /auth/verify flow with Testcontainers

**Acceptance**: All PKG-04 acceptance criteria pass

---

## Week 3: Complete Gateway & Test End-to-End Auth

### Day 11-13: Implement Gateway Auth Flow

#### Tasks:
1. **Implement AuthVerifyService**
   - File: `gateway/src/security/auth-verify.service.ts`
   - Currently: Returns null
   - Add:
     - HTTP client to `http://auth:8084/auth/verify`
     - LRU cache (10 seconds TTL)
     - Parse response: `{ valid, userId, email, roles, reason }`

2. **Implement GateMiddleware**
   - File: `gateway/src/security/gate.middleware.ts`
   - Currently: Just calls `next()`
   - Add full flow:
     1. Extract `Authorization: Bearer <token>`
     2. If token present → call `authVerifyService.verify(token)`
     3. If valid → set userId, roles; else → anonymous
     4. Extract operation name via `operationParser`
     5. Check if operation in `publicOperations` allowlist
     6. If protected + anonymous → return 401
     7. Sign request with `requestSigner.sign(userId, roles, requestId)`
     8. Add headers: `X-Gateway-Signature`, `X-User-Id`, `X-User-Roles`, `X-Request-Id`
     9. Forward to subgraph

3. **Test RequestSigner**
   - File: `gateway/src/security/request-signer.ts`
   - Already has HMAC logic, needs validation
   - Ensure: Matches PKG-02 SignatureVerifier expectations

4. **Configure Apollo Gateway**
   - File: `gateway/src/gateway/gateway.config.ts`
   - Add `IntrospectAndCompose` with 5 subgraph URLs
   - Configure polling interval

5. **Write tests**
   - `auth-verify.service.spec.ts`: HTTP client, cache hits/misses
   - `gate.middleware.spec.ts`: All 6 flow branches
   - `request-signer.spec.ts`: HMAC correctness
   - `gateway.e2e-spec.ts`: Full flow (unauthenticated public, unauthenticated protected, authenticated)

**Acceptance**: Gateway authenticates, gates, signs, and forwards requests correctly

---

### Day 14-15: Integration Testing

#### Tasks:
1. **Start all services locally**
   ```bash
   docker-compose up -d postgres kafka redis
   cd services && ./gradlew :auth:bootRun
   cd gateway && pnpm start:dev
   ```

2. **Test register → login → token flow**
   ```bash
   # Register
   curl -X POST http://localhost:4000/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "mutation { register(input: {email: \"test@example.com\", password: \"password123\", firstName: \"Test\", lastName: \"User\"}) { accessToken refreshToken } }"}'

   # Login
   curl -X POST http://localhost:4000/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "mutation { login(input: {email: \"test@example.com\", password: \"password123\"}) { accessToken refreshToken } }"}'

   # Use token
   curl -X POST http://localhost:4000/graphql \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <ACCESS_TOKEN>" \
     -d '{"query": "query { me { id email firstName lastName } }"}'
   ```

3. **Verify Auth Service /auth/verify endpoint**
   ```bash
   curl -X POST http://localhost:8084/auth/verify \
     -H "Content-Type: application/json" \
     -d '{"token": "<ACCESS_TOKEN>"}'
   ```

4. **Verify JWKS endpoint**
   ```bash
   curl http://localhost:8084/.well-known/jwks.json
   ```

5. **Test revocation**
   - Logout → verify token fails
   - Kafka consumer updates cache across instances

**Acceptance**: Full auth flow works end-to-end, register → login → use token → logout

---

## Week 4-6: Domain Services (Parallel Work)

After Week 3, all domain services can be built in parallel. Focus on critical path first.

### Priority 1: Cart Service (PKG-08) — 2 weeks
**Critical Path**: Blocks Order Service

#### Week 4-5 Tasks:
1. Implement CartRedisRepository (Redis HSET/HGETALL/EXPIRE)
2. Implement CatalogClient (validate products before adding)
3. Implement cart GraphQL resolvers (getCart, addToCart, updateQuantity, removeItem, clearCart)
4. Implement checkoutAndClear() RPC endpoint
5. Implement Kafka producers (cart.updated, cart.checked-out)
6. Write tests with Testcontainers Redis
7. Load test Redis performance (target: <5ms p99 for addToCart)

**Acceptance**: Users can add items to cart, update quantities, checkout

---

### Priority 2: Catalog Service (PKG-05) — 3 weeks (parallel)

#### Week 4-6 Tasks:
1. Implement Product, Variant, Collection models
2. Implement Flyway migrations (4 tables)
3. Implement repositories with JPA
4. Implement CatalogService business logic
5. Implement GraphQL resolvers (products, product, collections, collection)
6. Implement VariantDataLoader (N+1 prevention)
7. Implement admin mutations (@admin directive): createProduct, updateProduct, updateInventory
8. Implement Kafka producers (catalog.products, catalog.inventory)
9. Implement PostgreSQL full-text search for product search
10. Write tests

**Acceptance**: Users can browse products, admin can manage catalog, inventory events published

---

### Priority 3: Customer Service (PKG-07) — 2 weeks (parallel)

#### Week 4-5 Tasks:
1. Implement Customer, Address, Wishlist models
2. Implement Flyway migrations (3 tables)
3. Implement AuthEventConsumer (idempotent, creates customer from user.registered)
4. Implement CustomerEventProducer
5. Implement GraphQL resolvers (me, updateProfile, addAddress, updateAddress, removeAddress, addToWishlist, removeFromWishlist)
6. Implement address default logic (isDefault: true unsets previous default)
7. Write tests with Kafka Testcontainers

**Acceptance**: Customer created on user registration, profile management works

---

## Week 7-10: Orders & Streams (Critical Path Completion)

### Week 7-8: Order Service (PKG-06) — 2-3 weeks

#### Tasks:
1. Implement Order, OrderItem models
2. Implement Flyway migrations (2 tables)
3. Implement CheckoutOrchestrator:
   - Call Cart.checkoutAndClear()
   - Create Order (PENDING)
   - Publish OrderPlaced event
4. Implement OrderStatusSubscription (WebSocket live updates)
5. Implement sealed class hierarchy (OrderPlaced, OrderConfirmed, OrderShipped, OrderCancelled)
6. Implement InventoryResponseConsumer (updates order status from Streams)
7. Implement GraphQL resolvers (orders, order, checkout)
8. Implement federation: extend Customer with orders field
9. Write tests

**Acceptance**: Users can checkout, see orders, track order status in real-time

---

### Week 8-10: Kafka Streams (PKG-09) — 2-3 weeks (parallel)

#### Tasks:
1. Implement InventoryTopology:
   - Input: dachshaus.order.events (filter: OrderPlaced)
   - State: inventory-store (from catalog.inventory)
   - Logic: Check stock, deduct if available (all-or-nothing)
   - Output: InventoryReserved or InventoryFailed
2. Implement OrderEnrichmentTopology:
   - Join order + customer + product data
   - Output: enriched orders
3. Implement NotificationTopology:
   - Input: OrderConfirmed, OrderShipped
   - Output: notifications.outbox
4. Implement custom serdes (OrderEventSerde, ProductEventSerde, EnrichedOrderSerde)
5. Configure persistent RocksDB state stores
6. Enable exactly-once semantics (`exactly_once_v2`)
7. Write tests with TopologyTestDriver
8. Test DLQ (failed messages land with diagnostic headers)

**Acceptance**: Inventory reservation works, order enrichment works, notifications sent

---

## Week 11-13: Production Readiness

### Week 11-12: Storefront (PKG-10)

#### Tasks:
1. Implement auth context (login, logout, register, refresh, token storage)
2. Implement useProducts hook with actual Apollo query
3. Implement useCart hook with optimistic updates
4. Implement useOrderTracking hook with WebSocket subscription
5. Implement all pages:
   - Homepage: Featured collections
   - Products: Grid, pagination, filters
   - Product detail: Variant selector, add to cart
   - Cart: Full cart, quantity editing
   - Checkout: Address form, place order
   - Orders: Order history
   - Order detail: Live status updates
   - Login/Register forms
   - Admin: Product CRUD
6. Generate types from GraphQL schema
7. Test SSR behavior (view source shows data)

**Acceptance**: Full user flow works in browser

---

### Week 13: CI/CD & Testing

#### Tasks:
1. Add `terraform-plan.yml` workflow (PR comments with diff)
2. Create `.github/dependabot.yml`
3. Create `.github/CODEOWNERS`
4. Implement smoke tests in deploy workflow
5. Test full CI/CD pipeline:
   - PR → lint + test
   - Merge → build + push + deploy
   - Smoke tests pass
6. Test Kubernetes deployment (`kubectl apply -k k8s/overlays/dev`)
7. Test rollback (`kubectl rollout undo`)
8. Load testing (k6): 10k concurrent users, 100k cart ops/sec
9. Security review

**Acceptance**: Platform deployed to GCP, CI/CD working, smoke tests passing

---

## Summary Timeline

| Week | Focus | Deliverables |
|------|-------|-------------|
| 1 | PKG-02 + PKG-04 start | Common Module complete, JWT infrastructure working |
| 2 | PKG-04 complete | Full auth service (register, login, refresh, logout, /auth/verify) |
| 3 | PKG-03 + integration | Gateway complete, end-to-end auth flow working |
| 4-5 | PKG-08 + PKG-05 + PKG-07 | Cart, Catalog, Customer services (parallel) |
| 6 | PKG-05 complete | Catalog complete |
| 7-8 | PKG-06 | Order Service complete |
| 8-10 | PKG-09 | Kafka Streams complete |
| 11-12 | PKG-10 | Storefront complete |
| 13 | PKG-13 + testing | CI/CD complete, platform deployed |

**Total Timeline**: 13 weeks to MVP

---

## Success Metrics

### Milestone 1: Foundation (End of Week 3)
- ✅ PKG-02 complete with tests passing
- ✅ PKG-04 complete with JWT issuance working
- ✅ PKG-03 complete with auth flow working
- ✅ Register → Login → Token → /auth/verify flow proven

### Milestone 2: Core Services (End of Week 6)
- ✅ PKG-05 Catalog complete (browse products)
- ✅ PKG-07 Customer complete (profile management)
- ✅ PKG-08 Cart complete (add to cart)
- ✅ Users can browse products and add to cart

### Milestone 3: Checkout (End of Week 10)
- ✅ PKG-06 Order complete (checkout flow)
- ✅ PKG-09 Streams complete (inventory reservation)
- ✅ Users can checkout and see order status
- ✅ Live order updates via WebSocket

### Milestone 4: Production (End of Week 13)
- ✅ PKG-10 Storefront complete
- ✅ PKG-13 CI/CD complete
- ✅ Platform deployed to GCP
- ✅ All smoke tests passing
- ✅ Load tests passing (10k users, 100k cart ops/sec)

---

## Daily Standup Format (Recommended)

**Yesterday**:
- What tasks completed?
- What blockers encountered?

**Today**:
- What tasks in progress?
- Which acceptance criteria targeting?

**Risks**:
- Any new blockers?
- Any timeline slips?

---

## Tools & Commands

### Build & Run Locally
```bash
# Kotlin services
cd services && ./gradlew :auth:bootRun

# Gateway
cd gateway && pnpm install && pnpm start:dev

# Storefront
cd storefront && pnpm install && pnpm dev

# All infrastructure
docker-compose up -d
```

### Run Tests
```bash
# Kotlin
cd services && ./gradlew test

# Gateway
cd gateway && pnpm test

# Storefront
cd storefront && pnpm test
```

### Database Migrations
```bash
# Auth service migrations run automatically on startup
# Or manually: ./gradlew :auth:flywayMigrate
```

### Kafka Topics
```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker-compose exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic dachshaus.auth.user-registered \
  --partitions 3 \
  --replication-factor 1
```

---

## References

- **Detailed Status**: See `IMPLEMENTATION_STATUS.md`
- **Package Specs**: See `.claude/packages/pckg_*.md`
- **Architecture**: See `ARCHITECTURE_EXTENSION.md`
- **Installation**: See `INSTALL.md`
- **Quickstart**: See `QUICKSTART.md`

---

**Last Updated**: 2026-03-17
**Next Review**: Weekly (every Monday)
