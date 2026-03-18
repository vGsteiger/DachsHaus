# PKG-04: Auth Service

**Status:** 15% Complete (Bootstrap + migrations exist, zero business logic)
**Depends on:** PKG-01 ✅, PKG-02 (85% complete)
**Last Verified:** 2026-03-17
**CRITICAL PATH**: This package blocks PKG-03 (Gateway), PKG-07 (Customer), and PKG-09 (Streams)

## Goal

Dedicated authentication service — owns credentials, issues RSA-signed JWTs, provides `/verify` endpoint for gateway, handles token refresh and revocation.

## Produces

- REST API: `POST /auth/verify`, `GET /auth/.well-known/jwks.json`, `GET /healthz`
- GraphQL subgraph: `login`, `register`, `refreshToken`, `logout` mutations, `authStatus` query
- `TokenService` — RSA key loading, JWT issuance + verification, JWKS generation
- `AuthService` — register (BCrypt hash + save), login (verify + issue), refresh (rotate), revoke
- `RevocationService` — in-memory `ConcurrentHashMap` cache, warmed from DB, Kafka-synced
- `LoginThrottleService` — rate limiting: 5 attempts per email per 15 min, lockout after 10
- `AuthEventProducer` — publishes `user.registered` and `revocations` to Kafka
- Flyway migrations: `credentials`, `refresh_tokens`, `revocations` tables

## Interface Contracts

### REST (internal — consumed by Gateway)
```
POST /auth/verify
  Request:  { "token": "eyJhbG..." }
  Response 200: { "valid": true, "userId": "uuid", "email": "...", "roles": ["customer"] }
  Response 200: { "valid": false, "reason": "expired|revoked|invalid" }
  Note: always HTTP 200; consumers check the `valid` field
  Latency target: <5ms (in-memory revocation check + RSA verify)

GET /auth/.well-known/jwks.json
  Response: { "keys": [{ "kty": "RSA", "kid": "...", "use": "sig", ... }] }
```

### GraphQL (federation subgraph)
```graphql
mutation login(input: LoginInput!): AuthPayload!
mutation register(input: RegisterInput!): AuthPayload!
mutation refreshToken(token: String!): TokenPair!
mutation logout: Boolean!
query authStatus: AuthStatus!
```

### Kafka (produced)
- `dachshaus.auth.user-registered` → `{ userId, email, firstName, lastName, roles, timestamp }` (key: userId)
  - Consumed by: Customer Service (PKG-07)
- `dachshaus.auth.revocations` → `{ userId, action: "revoke"|"unrevoke", timestamp }` (key: userId)
  - Consumed by: Auth Service (self, all instances)

### JWT Specification
- Algorithm: RS256
- Claims: `sub` (userId UUID), `email`, `roles` (string[]), `iss` ("dachshaus-auth"), `iat`, `exp`
- Access token TTL: 15 minutes
- Refresh token TTL: 7 days (stored hashed in DB, single-use rotation)

### Password Hashing
- BCrypt cost factor 12 (~250ms per hash)

### Rate Limiting
- 5 failed attempts per email per 15 min → 429
- 10 consecutive failures → 30 min account lockout

## Acceptance Criteria

- [ ] `POST /auth/verify` returns valid result for a fresh JWT in <5ms ⚠️ **STUB ONLY**
- [ ] `POST /auth/verify` returns `{ valid: false, reason: "revoked" }` after revocation ⚠️ **NOT IMPLEMENTED**
- [ ] `register` creates credential, hashes password with BCrypt(12), publishes `user.registered` ⚠️ **NOT IMPLEMENTED**
- [ ] `login` returns 429 after 5 failed attempts within 15 minutes (per-email throttle) ⚠️ **NOT IMPLEMENTED**
- [ ] `refreshToken` rotates: old token revoked, new pair issued ⚠️ **NOT IMPLEMENTED**
- [ ] `logout` revokes all refresh tokens for user, publishes revocation event ⚠️ **NOT IMPLEMENTED**
- [ ] Revocation cache syncs across instances via Kafka consumer ⚠️ **NOT IMPLEMENTED**
- [ ] JWKS endpoint returns valid RSA public key in JWK format ⚠️ **STUB - returns empty array**
- [ ] Flyway migrations run cleanly on empty database ✅ **IMPLEMENTED**
- [ ] All tests pass: unit (TokenService, AuthService, Throttle), integration (VerifyController) ❌ **NO TESTS EXIST**

## Current Implementation Status

### ✅ Bootstrap & Migrations (15%)
- **AuthApplication.kt**: Spring Boot main class ✅
- **V1__create_credentials.sql**: Full table definition ✅
- **V2__create_refresh_tokens.sql**: Full table definition ✅
- **V3__create_revocations.sql**: Full table definition ✅

### ⚠️ Configuration (Empty)
- **JwtConfig.kt**: Empty @Configuration class (needs RSA keys, TTLs)
- **DatabaseConfig.kt**: Empty @Configuration class
- **SecurityConfig.kt**: Present

### ⚠️ REST Endpoints (Stubs Only)
- **HealthController.kt**: Returns `{status: "UP"}` ✅
- **VerifyController.kt**: Returns `{status: "ok"}` stub, **NO JWT VERIFICATION**
- **JwksController.kt**: Returns empty keys array `{keys: []}`, **NO RSA PUBLIC KEY**

### ⚠️ GraphQL (Stub Only)
- **AuthResolver.kt**: Only `login()` mutation returning empty token string
- **schema.graphqls**: Exists

### ✅ Domain Models (Defined)
- **Credential.kt**: Data class defined (id, email, passwordHash, roles) ✅

### ⚠️ Domain Services (Empty)
- **AuthService.kt**: Skeleton with comment placeholders only

### ⚠️ Kafka (Stub)
- **AuthEventProducer.kt**: Skeleton with comment placeholders

### ❌ Missing Entirely (27/37 files)
- TokenService (RSA key loading, JWT issue/verify, JWKS)
- RevocationService (cache + Kafka sync)
- LoginThrottleService (rate limiting)
- CredentialRepository, RefreshTokenRepository, RevocationRepository
- Entity classes (CredentialEntity, RefreshTokenEntity)
- Mappers (CredentialMapper)
- GraphQL mutations: register, refreshToken, logout, authStatus
- All test files (0 tests found)

## Remaining Work
1. **TokenService**: Generate RSA key pair, implement JWT issuance (RS256, 15min/7day TTL), implement verification, generate JWKS
2. **AuthService**: Implement register (BCrypt(12)), login (verify + issue tokens), refresh (rotate), revoke (logout)
3. **RevocationService**: In-memory ConcurrentHashMap cache, warm from DB on startup, Kafka consumer for sync
4. **LoginThrottleService**: 5 attempts/15min per email, 10 consecutive → 30min lockout
5. **Repositories**: JPA repositories for Credential, RefreshToken, Revocation
6. **Entity Classes**: JPA entities + mappers
7. **REST Endpoints**: Implement /auth/verify logic (JWT verify + revocation check)
8. **GraphQL Mutations**: Implement all 4 mutations (register, refreshToken, logout, authStatus)
9. **Kafka Producer**: Publish user.registered and revocations events
10. **Tests**: Unit tests (TokenService, AuthService, LoginThrottle), integration tests (VerifyController)
11. **RSA Keys**: Generate key pair and store in resources/keys/

## Critical Dependencies
- **Blocks PKG-03**: Gateway needs /auth/verify endpoint to authenticate requests
- **Blocks PKG-07**: Customer Service consumes user.registered events
- **Blocks PKG-09**: Streams consumes revocations events
- **Blocks ALL**: Without JWT tokens, no user operations work

## Files to Create

```
services/auth/build.gradle.kts
services/auth/src/main/kotlin/com/dachshaus/auth/AuthApplication.kt
services/auth/src/main/kotlin/com/dachshaus/auth/config/JwtConfig.kt
services/auth/src/main/kotlin/com/dachshaus/auth/config/SecurityConfig.kt
services/auth/src/main/kotlin/com/dachshaus/auth/config/DatabaseConfig.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/model/Credential.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/model/RefreshToken.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/model/TokenRevocation.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/repository/CredentialRepository.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/repository/RefreshTokenRepository.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/AuthService.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/TokenService.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/RevocationService.kt
services/auth/src/main/kotlin/com/dachshaus/auth/domain/service/LoginThrottleService.kt
services/auth/src/main/kotlin/com/dachshaus/auth/api/rest/VerifyController.kt
services/auth/src/main/kotlin/com/dachshaus/auth/api/rest/JwksController.kt
services/auth/src/main/kotlin/com/dachshaus/auth/api/rest/HealthController.kt
services/auth/src/main/kotlin/com/dachshaus/auth/api/graphql/resolver/AuthResolver.kt
services/auth/src/main/kotlin/com/dachshaus/auth/api/graphql/schema.graphqls
services/auth/src/main/kotlin/com/dachshaus/auth/kafka/AuthEventProducer.kt
services/auth/src/main/kotlin/com/dachshaus/auth/infrastructure/persistence/entity/CredentialEntity.kt
services/auth/src/main/kotlin/com/dachshaus/auth/infrastructure/persistence/entity/RefreshTokenEntity.kt
services/auth/src/main/kotlin/com/dachshaus/auth/infrastructure/persistence/mapper/CredentialMapper.kt
services/auth/src/main/resources/application.yml
services/auth/src/main/resources/application-local.yml
services/auth/src/main/resources/application-docker.yml
services/auth/src/main/resources/application-kubernetes.yml
services/auth/src/main/resources/keys/auth-public.pem
services/auth/src/main/resources/db/migration/V1__create_credentials.sql
services/auth/src/main/resources/db/migration/V2__create_refresh_tokens.sql
services/auth/src/main/resources/db/migration/V3__create_revocations.sql
services/auth/src/main/resources/graphql/schema.graphqls
services/auth/src/test/kotlin/com/dachshaus/auth/domain/service/AuthServiceTest.kt
services/auth/src/test/kotlin/com/dachshaus/auth/domain/service/TokenServiceTest.kt
services/auth/src/test/kotlin/com/dachshaus/auth/domain/service/LoginThrottleServiceTest.kt
services/auth/src/test/kotlin/com/dachshaus/auth/api/rest/VerifyControllerIntegrationTest.kt
services/auth/src/test/kotlin/com/dachshaus/auth/testcontainers/PostgresTestBase.kt
```
