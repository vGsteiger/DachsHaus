# PKG-02: Common Kotlin Module

**Status:** 100% Complete ✅
**Depends on:** PKG-01 ✅
**Blocks:** PKG-04, PKG-05, PKG-06, PKG-07, PKG-08, PKG-09
**Last Verified:** 2026-03-22

## Goal

Shared library consumed by all Kotlin services — gateway signature verification, auth directives, Kafka utilities.

## Produces

- `GatewaySignatureFilter` — servlet filter that verifies HMAC-SHA256 on every `/graphql` request
- `SignatureVerifier` — extracted HMAC logic with constant-time comparison
- `UserContext` — data class parsed from verified `x-user-id`, `x-user-roles`, `x-request-id` headers
- `SecurityConfig` — auto-registers the filter for all subgraphs
- `AuthDirective` — `@authenticated` GraphQL directive (rejects `x-user-id: anonymous`)
- `AdminDirective` — `@admin` directive (requires `admin` in roles)
- `ContextBuilder` — builds `UserContext` from HTTP headers for GraphQL resolvers
- `TopicNames` — object with constants for all 11 Kafka topic names
- `JsonSerde` — reusable Jackson-based Kafka serde
- `DeadLetterPublisher` — sends failed messages to DLQ with diagnostic headers

## Interface Contracts

### HTTP Headers (gateway → subgraph)
```
X-Gateway-Signature: {timestamp}.{hmac-sha256}
X-User-Id: {uuid | "anonymous"}
X-User-Roles: {comma-separated | "none"}
X-Request-Id: {uuid}
```

### HMAC Specification
- Payload: `"{x-user-id}:{x-user-roles}:{x-request-id}:{timestamp}"`
- Algorithm: HMAC-SHA256
- Timestamp skew tolerance: ±30 seconds
- Constant-time comparison (no early exit)

### Kafka Topic Names
All topics follow pattern `dachshaus.{domain}.{event}`:
- `dachshaus.auth.user-registered`
- `dachshaus.auth.revocations`
- `dachshaus.catalog.products`
- `dachshaus.catalog.inventory`
- `dachshaus.order.events`
- `dachshaus.order.enriched`
- `dachshaus.customer.events`
- `dachshaus.cart.updated`
- `dachshaus.cart.checked-out`
- `dachshaus.notifications.outbox`
- `dachshaus.dlq`

### DLQ Headers
```
dlq.source.topic
dlq.source.partition
dlq.source.offset
dlq.source.service
dlq.error.class
dlq.error.message
dlq.timestamp
```

## Acceptance Criteria

- [x] Filter rejects requests without `X-Gateway-Signature` with 403 ✅
- [x] Filter rejects requests with expired timestamps (>30s old) with 403 ✅
- [x] Filter rejects requests with tampered signatures with 403 ✅
- [x] Filter passes valid signed requests and populates `UserContext` ✅
- [x] `@authenticated` directive blocks anonymous requests ✅
- [x] `@admin` directive blocks non-admin requests ✅
- [x] `ContextBuilder` extracts UserContext from HTTP headers ✅
- [x] `DeadLetterPublisher` attaches all 7 diagnostic headers ✅
- [x] `SignatureVerifier` uses constant-time comparison (no early exit) ✅
- [x] GraphQL directive tests pass: unit tests for ContextBuilder, AuthDirective, AdminDirective ✅
- [x] GatewaySignatureFilter unit tests (11 test cases) ✅
- [x] SignatureVerifier unit tests (10 test cases) ✅

## Current Implementation Status

### ✅ All Complete (100%)
- **GatewaySignatureFilter.kt**: Full HMAC-SHA256 verification — missing header, bad format, expired timestamp, and invalid signature all return 403
- **SignatureVerifier.kt**: Constant-time XOR comparison via `MessageDigest`-style approach; `computeHmac` returns 64-char hex string
- **DeadLetterPublisher.kt**: Publishes to DLQ with all 7 diagnostic headers (source.topic, source.partition, source.offset, source.service, error.class, error.message, timestamp)
- **TopicNames.kt**: All 11 topic constants defined (auth ×2, catalog ×2, order ×2, customer ×1, cart ×2, notifications ×1, DLQ)
- **AuthDirective.kt**: Rejects userId == "anonymous", throws UnauthorizedException
- **AdminDirective.kt**: Requires "admin" in roles, throws UnauthorizedException
- **ContextBuilder.kt**: Header parsing (X-User-Id, X-User-Roles, X-Request-Id)
- **UserContext.kt**: Data class complete
- **SecurityConfig.kt**: Configuration present
- **JsonSerde.kt**: Present
- **Tests**: All test files present and passing

## Remaining Work
None.

## Files to Create

```
services/common/build.gradle.kts
services/common/src/main/kotlin/com/dachshaus/common/security/GatewaySignatureFilter.kt
services/common/src/main/kotlin/com/dachshaus/common/security/SignatureVerifier.kt
services/common/src/main/kotlin/com/dachshaus/common/security/UserContext.kt
services/common/src/main/kotlin/com/dachshaus/common/security/SecurityConfig.kt
services/common/src/main/kotlin/com/dachshaus/common/graphql/AuthDirective.kt
services/common/src/main/kotlin/com/dachshaus/common/graphql/AdminDirective.kt
services/common/src/main/kotlin/com/dachshaus/common/graphql/ContextBuilder.kt
services/common/src/main/kotlin/com/dachshaus/common/kafka/TopicNames.kt
services/common/src/main/kotlin/com/dachshaus/common/kafka/JsonSerde.kt
services/common/src/main/kotlin/com/dachshaus/common/kafka/DeadLetterPublisher.kt
services/common/src/test/kotlin/com/dachshaus/common/security/GatewaySignatureFilterTest.kt
services/common/src/test/kotlin/com/dachshaus/common/security/SignatureVerifierTest.kt
```
