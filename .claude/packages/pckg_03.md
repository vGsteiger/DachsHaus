# PKG-03: Federation Gateway

**Status:** Not Started
**Depends on:** PKG-01
**Consumes (runtime):** Auth Service `/auth/verify` endpoint (PKG-04)

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
Response 401: { "valid": false, "reason": "expired|revoked|invalid" }
```

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

- [ ] Gateway starts and composes supergraph from running subgraphs
- [ ] Unauthenticated request to public operation → signed and forwarded as anonymous
- [ ] Unauthenticated request to protected operation → 401 returned to client
- [ ] Authenticated request with valid token → Auth Service verify called, signed, forwarded with user context
- [ ] Authenticated request with invalid token → falls back to anonymous, gated by allowlist
- [ ] Cache hit: same token within 10s does not call Auth Service again
- [ ] `/healthz` returns 200
- [ ] All headers (signature, user-id, roles, request-id) present on forwarded requests

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
