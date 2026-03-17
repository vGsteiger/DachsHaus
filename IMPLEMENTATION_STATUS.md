# DachsHaus Implementation Status (Current State)

**Last Updated**: 2026-03-17
**Basis of Truth**: Actual codebase analysis (not IMPLEMENTATION_PLAN.md)

This document reflects the **actual implementation status** based on code review, not aspirational planning. Only code that exists and has real logic is counted as "complete".

---

## Executive Summary

- **Total Progress**: ~15% of full platform implementation
- **Scaffold Complete**: ✅ Directory structure, build systems, Docker, K8s, Terraform
- **Working Services**: 0 (all are scaffolds only)
- **Critical Blocker**: PKG-04 (Auth Service) - needed by all other services
- **Biggest Gap**: Business logic missing from all services and gateway

---

## Package Status Overview

| Package | Official Status | Actual Status | Files Present | Real Completion | Critical? |
|---------|----------------|---------------|---------------|-----------------|-----------|
| PKG-01 | ✅ Complete | ✅ Complete | ~100 | 100% | Yes |
| PKG-02 | 🟡 70% | 🟢 85% | 13 | High | Yes |
| PKG-03 | 🟡 85% | 🔴 60% | 16 | Medium-Low | Yes |
| PKG-04 | ❌ 0% | 🔴 15% | 10 | Low | **YES** |
| PKG-05 | ❌ 0% | 🔴 1% | 1 | Minimal | No |
| PKG-06 | ❌ 0% | 🔴 1% | 1 | Minimal | Yes |
| PKG-07 | ❌ 0% | 🔴 1% | 1 | Minimal | No |
| PKG-08 | ❌ 0% | 🔴 1% | 1 | Minimal | Yes |
| PKG-09 | ❌ 0% | 🔴 1% | 1 | Minimal | Yes |
| PKG-10 | 🟡 20% | 🟡 30% | 27 | Low | No |
| PKG-11 | ✅ Complete | ✅ Complete | 52 | Complete | No |
| PKG-12 | ✅ Complete | ✅ Complete | 23 | Complete | No |
| PKG-13 | 🟡 60% | 🟡 60% | 2 | Medium | No |

---

## PKG-02: Common Kotlin Module (85% Complete) 🟢

**Status**: Nearly complete, needs HMAC verification and DLQ implementation

### What's Implemented (10/13 files):

#### ✅ GraphQL Directives (FULLY WORKING)
- `AuthDirective.kt` - Checks userId != "anonymous", throws UnauthorizedException
- `AdminDirective.kt` - Checks "admin" in roles list, throws UnauthorizedException
- `ContextBuilder.kt` - Parses X-User-Id, X-User-Roles, X-Request-Id headers
- **Tests**: All 3 directive tests fully implemented and passing

#### ✅ Security Models
- `UserContext.kt` - Data class (userId, roles, requestId)
- `SecurityConfig.kt` - Configuration class

#### ⚠️ Security Verification (STUBS)
- `GatewaySignatureFilter.kt` - Skeleton structure exists, **no HMAC verification logic**
- `SignatureVerifier.kt` - Present but **needs constant-time comparison validation**

#### ✅ Kafka Utilities
- `TopicNames.kt` - 6 topic names defined (user.registered, order.placed, order.confirmed, payment.processed, inventory.reserved, cart.updated)
- `JsonSerde.kt` - Present

#### ⚠️ Kafka DLQ (STUB)
- `DeadLetterPublisher.kt` - Skeleton only, **no implementation**

### Remaining Work:
1. Implement HMAC-SHA256 verification in `GatewaySignatureFilter`
2. Implement `DeadLetterPublisher` with 7 diagnostic headers
3. Write integration tests for filter
4. Validate constant-time comparison in SignatureVerifier
5. Add missing topic names (5 more topics per spec)

### Blockers:
- None (can proceed independently)

---

## PKG-03: Federation Gateway (60% Complete) 🔴

**Status**: Files exist but lack business logic

### What's Implemented (13/18 source files):

#### ✅ Infrastructure
- `main.ts` - Entry point exists
- `app.module.ts` - Minimal module imports
- `gateway.module.ts` - Present
- `gateway.config.ts` - Present
- `health.controller.ts` - Returns 200 OK
- `health.module.ts` - Present
- `public-operations.ts` - List of public operations defined
- `tracing.plugin.ts` - Present

#### ⚠️ Security Layer (ALL STUBS)
- `gate.middleware.ts` - **Empty**: just calls `next()`, no auth logic
- `auth-verify.service.ts` - **Stub**: async verify() returns null
- `request-signer.ts` - **Basic**: has HMAC-SHA256 logic but untested
- `operation-parser.ts` - Present
- `security.module.ts` - Present

#### ❌ Tests (ALL PLACEHOLDERS)
- `auth-verify.service.spec.ts` - Empty test
- `gate.middleware.spec.ts` - Present
- `request-signer.spec.ts` - "should be defined" placeholder only
- `gateway.e2e-spec.ts` - Missing entirely

### What's Missing:
1. **GateMiddleware**: Extract token → call Auth verify → gate → sign → forward
2. **AuthVerifyService**: HTTP client to Auth Service + 10s LRU cache
3. **All test implementations** (currently all stubs)
4. Federation gateway composition logic
5. Error handling and retry logic

### Remaining Work:
1. Implement full auth flow in GateMiddleware (6 steps)
2. Implement AuthVerifyService with caching
3. Write 4 test suites (unit + e2e)
4. Configure Apollo Gateway with IntrospectAndCompose
5. Implement tracing plugin integration

### Blockers:
- **PKG-04 Auth Service** `/auth/verify` endpoint (runtime dependency)
- PKG-02 GatewaySignatureFilter for request signing

---

## PKG-04: Auth Service (15% Complete) 🔴 **CRITICAL PATH**

**Status**: Skeleton exists, zero business logic

### What's Implemented (10/37 files):

#### ✅ Bootstrap
- `AuthApplication.kt` - Spring Boot main class

#### ⚠️ Configuration (ALL EMPTY)
- `JwtConfig.kt` - Empty @Configuration class (needs RSA keys, TTLs)
- `DatabaseConfig.kt` - Empty @Configuration class
- `SecurityConfig.kt` - Present

#### ⚠️ REST Endpoints (ALL STUBS)
- `HealthController.kt` - Returns `{status: "UP"}` ✅
- `VerifyController.kt` - Returns `{status: "ok"}` stub, **no JWT verification**
- `JwksController.kt` - Returns empty keys array `{keys: []}`, **no RSA public key**

#### ⚠️ GraphQL (STUB)
- `AuthResolver.kt` - Only `login()` mutation returning empty token string
- `schema.graphqls` - Exists

#### ✅ Domain Models
- `Credential.kt` - Data class defined (id, email, passwordHash, roles)

#### ⚠️ Domain Services (EMPTY)
- `AuthService.kt` - Skeleton with comment placeholders only

#### ⚠️ Kafka (STUB)
- `AuthEventProducer.kt` - Skeleton with comment placeholders

#### ✅ Database Migrations
- `V1__create_credentials.sql` - ✅ Full table definition
- `V2__create_refresh_tokens.sql` - ✅ Full table definition
- `V3__create_revocations.sql` - ✅ Full table definition

### What's Missing (27/37 files):
1. **TokenService** - RSA key loading, JWT issuance, verification, JWKS
2. **RevocationService** - In-memory cache + Kafka sync
3. **LoginThrottleService** - Rate limiting (5/15min, lockout after 10)
4. **Repositories**: CredentialRepository, RefreshTokenRepository, RevocationRepository
5. **GraphQL Mutations**: register, refreshToken, logout, authStatus
6. **REST Implementation**: Actual JWT verification in /auth/verify
7. **Kafka Producer**: user.registered, revocations events
8. **All Tests** (0 test files found)
9. **Entity Classes**: CredentialEntity, RefreshTokenEntity
10. **Mappers**: CredentialMapper

### Remaining Work:
1. Implement TokenService (RSA key gen, JWT issue/verify, JWKS endpoint)
2. Implement AuthService (register with BCrypt(12), login, refresh, revoke)
3. Implement RevocationService with Kafka consumer
4. Implement LoginThrottleService
5. Create all repository interfaces + JPA implementations
6. Implement GraphQL mutations (4 mutations)
7. Implement /auth/verify endpoint logic
8. Write comprehensive test suite
9. Generate RSA key pair

### Blockers:
- None (can proceed independently)

### Critical Impact:
- **Blocks PKG-03**: Gateway needs /auth/verify endpoint
- **Blocks PKG-07**: Customer Service consumes user.registered events
- **Blocks PKG-09**: Streams needs revocation events
- **Blocks ALL**: No authentication means no user operations work

---

## PKG-05: Catalog Service (1% Complete) 🔴

**Status**: Bootstrap only

### What's Implemented (1/32 files):
- `CatalogApplication.kt` - Spring Boot main class

### What's Missing (31/32 files):
- All GraphQL resolvers (products, collections, variants)
- All domain models (Product, Variant, Collection)
- All repositories
- VariantDataLoader for N+1 prevention
- CatalogService business logic
- Kafka producers (catalog.products, catalog.inventory)
- Flyway migrations (4 tables)
- All tests

### Remaining Work:
- Implement entire service from scratch (32 files)

### Blockers:
- PKG-02 Common Module for @admin directive

---

## PKG-06: Order Service (1% Complete) 🔴 **CRITICAL PATH**

**Status**: Bootstrap only

### What's Implemented (1/38 files):
- `OrderApplication.kt` - Spring Boot main class

### What's Missing (37/38 files):
- CheckoutOrchestrator
- OrderStatusSubscription (WebSocket)
- Sealed class hierarchy (OrderPlaced, OrderConfirmed, etc.)
- InventoryResponseConsumer
- All repositories, models, resolvers
- Flyway migrations (2 tables)
- All tests

### Remaining Work:
- Implement entire service from scratch (38 files)

### Blockers:
- **PKG-08 Cart Service** - needs Cart.checkoutAndClear() RPC
- PKG-02 Common Module for @authenticated directive

---

## PKG-07: Customer Service (1% Complete) 🔴

**Status**: Bootstrap only

### What's Implemented (1/28 files):
- `CustomerApplication.kt` - Spring Boot main class

### What's Missing (27/28 files):
- AuthEventConsumer (idempotent)
- CustomerEventProducer
- All GraphQL resolvers (me, updateProfile, addresses, wishlist)
- All domain models, repositories, services
- Flyway migrations (3 tables)
- All tests

### Remaining Work:
- Implement entire service from scratch (28 files)

### Blockers:
- **PKG-04 Auth Service** - consumes user.registered events (runtime)
- PKG-02 Common Module

---

## PKG-08: Cart Service (1% Complete) 🔴 **CRITICAL PATH**

**Status**: Bootstrap only

### What's Implemented (1/23 files):
- `CartApplication.kt` - Spring Boot main class

### What's Missing (22/23 files):
- CartRedisRepository (HSET, HGETALL, EXPIRE operations)
- CatalogClient (product validation)
- CartSnapshotService (async PostgreSQL dump)
- All GraphQL resolvers
- Kafka producers (cart.updated, cart.checked-out)
- Internal RPC: checkoutAndClear()
- All tests (including Testcontainers Redis)

### Remaining Work:
- Implement entire service from scratch (23 files)

### Blockers:
- PKG-02 Common Module
- **PKG-05 Catalog Service** - needs CatalogClient for product validation

---

## PKG-09: Kafka Streams (1% Complete) 🔴 **CRITICAL PATH**

**Status**: Bootstrap only

### What's Implemented (1/23 files):
- `StreamsApplication.kt` - Spring Boot main class

### What's Missing (22/23 files):
- InventoryTopology (stock reservation)
- OrderEnrichmentTopology (joins)
- NotificationTopology
- Custom serdes (OrderEventSerde, ProductEventSerde, EnrichedOrderSerde)
- InventoryStoreConfig (persistent RocksDB)
- All TopologyTestDriver tests

### Remaining Work:
- Implement entire streams application from scratch (23 files)

### Blockers:
- **PKG-04, 05, 06, 07, 08** - consumes events from all services (runtime)

---

## PKG-10: Storefront (30% Complete) 🟡

**Status**: Files exist but mostly shells/stubs

### What's Implemented (27 files):

#### ✅ Layout Infrastructure
- `app/layout.tsx` - ApolloProvider + AuthProvider wired ✅
- `app/page.tsx` - Minimal homepage ("Welcome to DachsHaus")
- `app/products/page.tsx` - Present
- `app/products/[slug]/page.tsx` - Present
- `app/globals.css` - Present

#### ✅ Components (Shell)
- `components/layout/Layout.tsx` - Present
- `components/layout/Header.tsx` - Present
- `components/layout/Footer.tsx` - Present
- `components/product/ProductCard.tsx` - Present
- `components/product/ProductGrid.tsx` - Present

#### ✅ GraphQL Operations (Defined)
- All queries defined (products, collections, cart, orders)
- All mutations defined (auth, cart, checkout)
- Subscription defined (orderStatus)

#### ⚠️ Auth Context (MINIMAL)
- `lib/auth/context.tsx` - Empty AuthContext, empty AuthProvider value
- `lib/auth/hooks.ts` - Present
- `lib/auth/storage.ts` - Present

#### ✅ Apollo Client (Configured)
- `lib/apollo/client.ts` - Present
- `lib/apollo/provider.tsx` - Present
- `lib/apollo/links.ts` - Present

#### ✅ Hooks (Defined)
- `hooks/useProducts.ts` - Present
- `hooks/useCart.ts` - Present
- `hooks/useOrderTracking.ts` - Present

### What's Missing:
- All page logic (currently just shells)
- Auth flow implementation (login/register/logout/refresh)
- Cart state management and optimistic updates
- Order tracking with WebSocket subscription
- Product detail page logic
- Checkout flow
- Admin product CRUD pages
- SSR data fetching
- Type generation from schema

### Remaining Work:
1. Implement auth context (login, logout, refresh, token storage)
2. Implement all hooks with actual Apollo queries
3. Implement all pages with data fetching and state
4. Connect components to Apollo Client
5. Implement optimistic updates for cart
6. Implement WebSocket subscription for order tracking
7. Generate types from GraphQL schema
8. Test SSR behavior

### Blockers:
- **PKG-03 Gateway** - needs working federation gateway (runtime)

---

## PKG-11: Kubernetes (Complete) ✅

**Status**: All manifests present and organized

### What's Implemented:
- 52 Kubernetes YAML files
- Namespaces, network policies, Istio configs
- Per-service: Deployment, Service, HPA, ServiceAccount
- Strimzi Kafka CRDs
- Kustomize overlays (dev/prod)
- External Secrets integration

### Remaining Work:
1. Test `kubectl apply -k k8s/overlays/dev`
2. Verify network policies
3. Verify Istio mTLS enforcement
4. Test HPA scaling
5. Verify External Secrets integration

### Blockers:
- None (can test independently)

---

## PKG-12: Terraform (Complete) ✅

**Status**: All modules present

### What's Implemented:
- 10 Terraform modules (vpc, gke, cloud-sql, memorystore, secret-manager, artifact-registry, dns, monitoring, kafka)
- Environment configs (dev.tfvars, prod.tfvars)
- Output and variable definitions

### Remaining Work:
1. Test `terraform plan` for each environment
2. Validate GCP credentials
3. Review cost estimates

### Blockers:
- None

---

## PKG-13: CI/CD (60% Complete) 🟡

**Status**: Basic workflows present, needs expansion

### What's Implemented:
- `.github/workflows/ci.yml` - Present
- `.github/workflows/deploy.yml` - Present
- `.github/workflows/release.yml` - Present

### What's Missing:
- `terraform-plan.yml` workflow (PR comments)
- `.github/dependabot.yml`
- `.github/CODEOWNERS`
- Smoke tests
- Rollback documentation

### Remaining Work:
1. Add terraform-plan workflow
2. Create dependabot config
3. Create CODEOWNERS
4. Implement smoke tests in deploy workflow
5. Test full CI/CD pipeline
6. Document rollback procedure

### Blockers:
- None (can proceed independently)

---

## Critical Path Analysis

### Current Critical Path (Must be completed in order):

```
PKG-02 (finish) → PKG-04 (Auth) → PKG-08 (Cart) → PKG-06 (Order) → PKG-09 (Streams)
      ↓
   PKG-03 (Gateway)
```

### Time Estimates (Realistic):

| Package | Remaining Work | Estimated Time |
|---------|---------------|----------------|
| PKG-02 | 15% | 2-3 days |
| PKG-04 | 85% | 2-3 weeks |
| PKG-03 | 40% | 1-2 weeks |
| PKG-08 | 99% | 2 weeks |
| PKG-06 | 99% | 2-3 weeks |
| PKG-09 | 99% | 2-3 weeks |

**Critical Path Duration**: ~10-13 weeks

### Parallelization Opportunities:

After PKG-02 + PKG-04 complete:
- PKG-05 (Catalog) - 3 weeks
- PKG-07 (Customer) - 2 weeks
- PKG-08 (Cart) - 2 weeks

All can run in parallel.

---

## Risk Assessment

### High-Risk Items:

1. **PKG-04 Auth Service**: Blocks everything, complex JWT + revocation logic
2. **PKG-03 Gateway**: Middleware logic is completely missing, needs full rewrite
3. **PKG-02 HMAC Filter**: Security-critical, needs constant-time comparison
4. **PKG-09 Streams**: Kafka Streams complexity, exactly-once semantics

### Medium-Risk Items:

1. **PKG-08 Redis Performance**: O(1) operations, need load testing
2. **PKG-06 WebSocket Subscriptions**: Live order updates, connection handling
3. **PKG-10 Auth Context**: Refresh token flow, HttpOnly cookies

---

## Next Steps (Prioritized)

### Week 1: Unblock Critical Path

1. **Complete PKG-02** (2-3 days)
   - Implement GatewaySignatureFilter HMAC verification
   - Implement DeadLetterPublisher
   - Write integration tests
   - Add missing 5 topic names

2. **Start PKG-04 Auth Service** (begin)
   - Implement TokenService with RSA key generation
   - Implement AuthService with BCrypt hashing
   - Implement /auth/verify endpoint

### Week 2-3: Auth Service + Gateway

3. **Complete PKG-04** (2-3 weeks)
   - Complete all 27 remaining files
   - RevocationService with Kafka sync
   - LoginThrottleService
   - All GraphQL mutations
   - Comprehensive test suite

4. **Complete PKG-03 Gateway** (parallel after PKG-02)
   - Implement GateMiddleware auth flow
   - Implement AuthVerifyService with cache
   - Write all 4 test suites
   - Configure Apollo Gateway

### Week 4-6: Domain Services

5. **Complete PKG-08 Cart Service** (2 weeks)
   - All Redis operations
   - CatalogClient integration
   - Performance testing

6. **Complete PKG-05 Catalog Service** (3 weeks, parallel)
   - All GraphQL resolvers
   - VariantDataLoader
   - Admin mutations

7. **Complete PKG-07 Customer Service** (2 weeks, parallel)
   - AuthEventConsumer
   - Profile/address/wishlist resolvers

### Week 7-10: Orders & Streams

8. **Complete PKG-06 Order Service** (2-3 weeks)
   - CheckoutOrchestrator
   - WebSocket subscriptions
   - InventoryResponseConsumer

9. **Complete PKG-09 Streams** (2-3 weeks, parallel)
   - InventoryTopology
   - OrderEnrichmentTopology
   - NotificationTopology

### Week 11-13: Production Readiness

10. **Complete PKG-10 Storefront** (3 weeks)
    - All pages and components
    - Auth flow
    - Cart state management
    - Order tracking

11. **Finalize PKG-13 CI/CD** (1 week)
    - Terraform plan workflow
    - Smoke tests
    - Full pipeline testing

12. **Integration Testing** (1 week)
    - End-to-end user flows
    - Load testing
    - Security review

---

## Success Criteria

### Milestone 1: Foundation Complete (Week 3)
- [ ] PKG-02 complete with all tests passing
- [ ] PKG-04 complete with JWT issuance working
- [ ] PKG-03 Gateway can authenticate requests
- [ ] Register → Login → Token flow working

### Milestone 2: Core Services (Week 6)
- [ ] PKG-05 Catalog complete
- [ ] PKG-07 Customer complete
- [ ] PKG-08 Cart complete
- [ ] Users can browse products and add to cart

### Milestone 3: Checkout Flow (Week 10)
- [ ] PKG-06 Order complete
- [ ] PKG-09 Streams complete
- [ ] Users can checkout and see order status
- [ ] Inventory reservation working

### Milestone 4: Production Ready (Week 13)
- [ ] PKG-10 Storefront complete
- [ ] PKG-13 CI/CD complete
- [ ] Platform deployed to GCP
- [ ] All smoke tests passing

---

## Conclusion

**Current Reality**: The project has excellent infrastructure scaffolding (Docker, K8s, Terraform, build systems) but minimal business logic. The IMPLEMENTATION_PLAN.md significantly overstated progress on PKG-03 (gateway tests don't exist) and understated progress on PKG-02 (directives are complete).

**Critical Blocker**: PKG-04 Auth Service is the most urgent priority. Without it, nothing else can function (no tokens, no authentication, no user context).

**Realistic Timeline**: 10-13 weeks to MVP with all critical services working.

**Recommendation**: Focus on critical path (PKG-02 → PKG-04 → PKG-03) before attempting domain services. Do not start PKG-05-09 until auth flow is proven working end-to-end.
