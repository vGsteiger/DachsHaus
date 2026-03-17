# PKG-02: Common Kotlin Module

**Status:** 85% Complete (GraphQL directives fully implemented, HMAC filter stubbed)
**Depends on:** PKG-01 ✅
**Blocks:** PKG-04, PKG-05, PKG-06, PKG-07, PKG-08, PKG-09
**Last Verified:** 2026-03-17

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

- [ ] Filter rejects requests without `X-Gateway-Signature` with 403 ⚠️ **STUB**
- [ ] Filter rejects requests with expired timestamps (>30s old) with 403 ⚠️ **STUB**
- [ ] Filter rejects requests with tampered signatures with 403 ⚠️ **STUB**
- [ ] Filter passes valid signed requests and populates `UserContext` ⚠️ **STUB**
- [x] `@authenticated` directive blocks anonymous requests ✅ **IMPLEMENTED**
- [x] `@admin` directive blocks non-admin requests ✅ **IMPLEMENTED**
- [x] `ContextBuilder` extracts UserContext from HTTP headers ✅ **IMPLEMENTED**
- [ ] `DeadLetterPublisher` attaches all 7 diagnostic headers ⚠️ **STUB**
- [ ] `SignatureVerifier` uses constant-time comparison (no early exit) ⚠️ **NEEDS VERIFICATION**
- [x] GraphQL directive tests pass: unit tests for ContextBuilder, AuthDirective, AdminDirective ✅ **PASSING**

## Current Implementation Status

### ✅ Completed (85%)
- **AuthDirective.kt**: Fully implemented with userId != "anonymous" check, throws UnauthorizedException
- **AdminDirective.kt**: Fully implemented with "admin" role check, throws UnauthorizedException
- **ContextBuilder.kt**: Fully implemented with header parsing (X-User-Id, X-User-Roles, X-Request-Id)
- **UserContext.kt**: Data class complete
- **SecurityConfig.kt**: Configuration present
- **TopicNames.kt**: 6 topics defined (needs 5 more per spec)
- **JsonSerde.kt**: Present
- **Tests**: AuthDirectiveTest, AdminDirectiveTest, ContextBuilderTest all implemented and passing

### ⚠️ Stubbed/Incomplete (15%)
- **GatewaySignatureFilter.kt**: Skeleton structure exists, NO HMAC verification logic
- **SignatureVerifier.kt**: Present but needs constant-time comparison validation
- **DeadLetterPublisher.kt**: Skeleton only, NO implementation

## Remaining Work
1. Implement HMAC-SHA256 verification in GatewaySignatureFilter
2. Validate constant-time comparison in SignatureVerifier
3. Implement DeadLetterPublisher with 7 diagnostic headers
4. Add missing 5 topic names to TopicNames
5. Write integration tests for GatewaySignatureFilter
6. Write unit tests for SignatureVerifier

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
