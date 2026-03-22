# PKG-03: Federation Gateway

**Status:** 25% Complete (File scaffold exists; auth middleware and auth-verify service are stubs)
**Depends on:** PKG-01 ✅
**Consumes (runtime):** Auth Service `/auth/verify` endpoint (PKG-04) ⚠️ **BLOCKS RUNTIME**
**Last Verified:** 2026-03-22

## Goal

NestJS application that composes all subgraphs via Apollo Federation, authenticates requests via the Auth Service, and signs forwarded requests with HMAC.

## Produces

- `GatewayModule` — Apollo Gateway with `IntrospectAndCompose`, 5 subgraph URLs, polling
- `GateMiddleware` — orchestrates: extract token → call `/auth/verify` → gate → sign → forward
- `AuthVerifyService` — HTTP client to Auth Service with 10-second in-process LRU cache
- `RequestSigner` — HMAC-SHA256 signing of `userId:roles:requestId:timestamp`
- `OperationParser` — extracts GraphQL operation name from query string
- `public-operations.ts` — `Set<string>` allowlist of unauthenticated operations
- `HealthController` — `/health` and `/healthz` for Kubernetes probes
- `TracingPlugin` — Apollo Server plugin forwarding spans to GCP Cloud Trace

## Interface Contracts

### Inbound
```
Client → Gateway: Authorization: Bearer {jwt} (optional)
Gateway listens on :4000/graphql
```

### Outbound to Auth Service
```
POST http://auth:8084/auth/verify
Body: { "token": "eyJ..." }
Response 200: { "valid": true, "userId": "uuid", "email": "...", "roles": ["customer"] }
Response 200: { "valid": false, "reason": "expired|revoked|invalid" }
```
Note: Auth Service always returns HTTP 200; gateway checks the `valid` field to determine authentication state.

### Outbound to Subgraphs
Forwarded headers: `X-Gateway-Signature`, `X-User-Id`, `X-User-Roles`, `X-Request-Id`

### Public Operations Allowlist
```
GetProducts, GetProduct, GetCollections, GetCollection,
Login, Register, RefreshToken, IntrospectionQuery
```

### Request Flow
1. Extract `Authorization: Bearer <token>` from request
2. If token present → call Auth Service `/auth/verify` (with LRU cache)
3. If valid → extract userId, roles; if invalid → treat as anonymous
4. Check if operation is in public allowlist
5. If protected operation + anonymous → return 401
6. Sign request with HMAC-SHA256
7. Forward to subgraph with security headers

## Acceptance Criteria

- [ ] Gateway starts and composes supergraph from running subgraphs ⚠️ **NOT IMPLEMENTED**
- [ ] Unauthenticated request to public operation → signed and forwarded as anonymous ⚠️ **NOT IMPLEMENTED**
- [ ] Unauthenticated request to protected operation → 401 returned to client ⚠️ **NOT IMPLEMENTED**
- [ ] Authenticated request with valid token → Auth Service verify called, signed, forwarded with user context ⚠️ **NOT IMPLEMENTED**
- [ ] Authenticated request with invalid token → falls back to anonymous, gated by allowlist ⚠️ **NOT IMPLEMENTED**
- [ ] Cache hit: same token within 10s does not call Auth Service again ⚠️ **NOT IMPLEMENTED**
- [ ] `/healthz` returns 200 ✅ **IMPLEMENTED**
- [ ] All headers (signature, user-id, roles, request-id) present on forwarded requests ⚠️ **NOT IMPLEMENTED**

## Current Implementation Status

### ✅ Infrastructure Complete (40%)
- **main.ts**: Entry point exists
- **app.module.ts**: Minimal module imports
- **gateway.module.ts**: Present
- **gateway.config.ts**: Present
- **health.controller.ts**: Returns 200 OK ✅
- **health.module.ts**: Present
- **public-operations.ts**: List of public operations defined
- **tracing.plugin.ts**: Present

### ⚠️ Security Layer Stubbed (20%)
- **gate.middleware.ts**: **EMPTY** - just calls `next()`, no auth logic
- **auth-verify.service.ts**: **STUB** - async verify() returns null, no cache, no HTTP call
- **request-signer.ts**: **BASIC** - has HMAC-SHA256 logic but untested
- **operation-parser.ts**: Present
- **security.module.ts**: Present

### ❌ Tests Missing (0%)
- **auth-verify.service.spec.ts**: Empty test file
- **gate.middleware.spec.ts**: Present but needs implementation
- **request-signer.spec.ts**: "should be defined" placeholder only
- **gateway.e2e-spec.ts**: ❌ **MISSING ENTIRELY**

## Remaining Work
1. Implement full auth flow in GateMiddleware:
   - Extract Authorization header
   - Call AuthVerifyService
   - Check operation allowlist
   - Gate protected operations
   - Sign request with HMAC
   - Forward with security headers
2. Implement AuthVerifyService:
   - HTTP client to Auth Service /auth/verify
   - 10-second LRU cache
   - Error handling
3. Configure Apollo Gateway with IntrospectAndCompose
4. Write all 4 test suites (3 unit + 1 e2e)
5. Test RequestSigner HMAC logic
6. Implement tracing plugin GCP integration

## Files to Create

```
gateway/package.json
gateway/tsconfig.json
gateway/nest-cli.json
gateway/.env
gateway/src/main.ts
gateway/src/app.module.ts
gateway/src/gateway/gateway.module.ts
gateway/src/gateway/gateway.config.ts
gateway/src/gateway/public-operations.ts
gateway/src/security/security.module.ts
gateway/src/security/auth-verify.service.ts
gateway/src/security/gate.middleware.ts
gateway/src/security/request-signer.ts
gateway/src/security/operation-parser.ts
gateway/src/health/health.controller.ts
gateway/src/tracing/tracing.plugin.ts
gateway/test/security/gate.middleware.spec.ts
gateway/test/security/request-signer.spec.ts
gateway/test/security/auth-verify.service.spec.ts
gateway/test/gateway.e2e-spec.ts
```
