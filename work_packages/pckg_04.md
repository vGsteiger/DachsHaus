# PKG-04: Auth Service

**Status:** Not Started
**Depends on:** PKG-01, PKG-02

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
  Response: { "valid": true, "userId": "uuid", "email": "...", "roles": ["customer"] }
            { "valid": false, "reason": "expired|revoked|invalid" }
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

- [ ] `POST /auth/verify` returns valid result for a fresh JWT in <5ms
- [ ] `POST /auth/verify` returns `{ valid: false, reason: "revoked" }` after revocation
- [ ] `register` creates credential, hashes password with BCrypt(12), publishes `user.registered`
- [ ] `login` returns 401 after 5 failures within 15 minutes
- [ ] `refreshToken` rotates: old token revoked, new pair issued
- [ ] `logout` revokes all refresh tokens for user, publishes revocation event
- [ ] Revocation cache syncs across instances via Kafka consumer
- [ ] JWKS endpoint returns valid RSA public key in JWK format
- [ ] Flyway migrations run cleanly on empty database
- [ ] All tests pass: unit (TokenService, AuthService, Throttle), integration (VerifyController)

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
