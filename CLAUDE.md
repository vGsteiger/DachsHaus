# DachsHaus — Project Instructions for Claude Code

## Overview

DachsHaus is an e-commerce platform built as a polyglot monorepo:
- **Backend services** (Kotlin/Spring Boot): Auth, Catalog, Order, Customer, Cart, Kafka Streams
- **Gateway** (TypeScript/NestJS): Apollo Federation gateway
- **Storefront** (TypeScript/Next.js): SSR frontend with Apollo Client
- **Infrastructure**: Kubernetes (Kustomize), Terraform (GCP), GitHub Actions CI/CD

## Architecture

```
Client → Next.js Storefront → Federation Gateway (NestJS)
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              Auth Service    Catalog Service   Cart Service (Redis)
              Order Service   Customer Service  Kafka Streams
                    │               │               │
                    └───────── Kafka (KRaft) ────────┘
```

All Kotlin services share a common library (`services/common/`) for security (HMAC signature verification), GraphQL directives (`@authenticated`, `@admin`), and Kafka utilities (serdes, DLQ publisher, topic names).

The gateway authenticates via the Auth Service's `/auth/verify` endpoint, then HMAC-signs all forwarded requests to subgraphs.

## Implementation Packages

Work is organized into 13 packages (PKG-01 through PKG-13). Each has a detailed spec in `.claude/packages/`. The implementation order is:

1. **Phase 1 (parallel):** PKG-01 Monorepo Scaffold, PKG-12 Terraform
2. **Phase 2 (after PKG-01):** PKG-02 Common Module, PKG-03 Gateway, PKG-10 Storefront shell
3. **Phase 3 (after PKG-02):** PKG-04 Auth, PKG-05 Catalog, PKG-07 Customer, PKG-08 Cart
4. **Phase 4 (after Phase 3):** PKG-06 Order, PKG-09 Streams
5. **Phase 5 (after all):** PKG-11 Kubernetes, PKG-13 CI/CD, PKG-10 Storefront complete

**Critical path:** PKG-01 → PKG-02 → PKG-04 + PKG-08 → PKG-06 → PKG-09

## How to Use the Package Specs

Each file in `.claude/packages/` is a self-contained work package. When starting a package:

1. Read the package spec file completely
2. Check its dependencies — ensure those packages are done first
3. Create all listed files
4. Meet all acceptance criteria
5. Run tests if applicable

## Key Conventions

### Kotlin Services
- Spring Boot 3.x, Kotlin, JRE 21
- Gradle build with shared conventions plugin (`buildSrc/dachshaus.service-conventions.gradle.kts`)
- DGS framework for GraphQL (federation v2)
- Flyway for database migrations
- Testcontainers for integration tests
- Package structure: `config/`, `domain/model/`, `domain/repository/`, `domain/service/`, `graphql/resolver/`, `kafka/`, `infrastructure/persistence/`

### TypeScript Services
- pnpm workspaces, Turborepo
- NestJS for gateway, Next.js 14 (App Router) for storefront
- Shared tsconfig and eslint packages

### Security Model
- Gateway signs all requests with HMAC-SHA256: `X-Gateway-Signature: {timestamp}.{hmac}`
- HMAC payload: `{x-user-id}:{x-user-roles}:{x-request-id}:{timestamp}`
- All subgraphs verify signature via `GatewaySignatureFilter` from common module
- JWTs: RS256, 15min access / 7day refresh, issued by Auth Service

### Kafka
- Topic naming: `dachshaus.{domain}.{event}`
- DLQ: `dachshaus.dlq` with 7 diagnostic headers
- Streams: exactly-once semantics, persistent RocksDB state stores
- All topic names defined in `TopicNames` object in common module

### Databases
- Each service owns its own PostgreSQL database (4 total: auth, catalog, order, customer)
- Cart uses Redis (hash per user, 30-day TTL)
- No cross-database queries — services communicate via GraphQL federation or Kafka

### Docker
- Kotlin: multi-stage (gradle build → JRE 21 slim)
- Node: multi-stage (pnpm install → node 20 alpine)

## File Structure

```
dachshaus/
├── .claude/packages/          # Work package specs (you are here)
├── services/                  # Kotlin services (Gradle)
│   ├── common/                # Shared library
│   ├── auth/
│   ├── catalog/
│   ├── order/
│   ├── customer/
│   ├── cart/
│   └── streams/
├── gateway/                   # NestJS federation gateway
├── storefront/                # Next.js frontend
├── packages/                  # Shared TS packages
│   ├── tsconfig/
│   ├── eslint-config/
│   └── graphql-schema/
├── infra/
│   ├── docker/                # Dockerfiles
│   └── terraform/             # GCP IaC
├── k8s/                       # Kubernetes manifests
│   ├── base/
│   ├── overlays/
│   └── scripts/
└── .github/workflows/         # CI/CD
```
