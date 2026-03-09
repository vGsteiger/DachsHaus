# DachsHaus 🦡🏠

> A polyglot microservices e-commerce platform demonstrating modern cloud-native architectures, GraphQL federation, event-driven patterns, and production-grade security practices.

**⚠️ This is a toy project built for exploring and testing technologies, not for production use.**

## 🎯 Project Overview

DachsHaus is a fully-featured e-commerce platform built as a polyglot monorepo, showcasing how multiple technologies can work together in a microservices architecture. The project demonstrates:

- **Polyglot architecture**: Kotlin (Spring Boot) for backend services, TypeScript for gateway and frontend
- **GraphQL Federation**: Apollo Federation v2 with multiple subgraphs
- **Event-driven architecture**: Kafka for inter-service communication
- **Microservices patterns**: Service isolation, CQRS, event sourcing
- **Production-ready security**: HMAC request signing, JWT authentication, role-based access control
- **Cloud-native**: Kubernetes-ready with Terraform IaC for GCP
- **Modern DevOps**: GitHub Actions CI/CD, containerized development

## 🏗️ Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                           Client                                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                ┌───────────▼──────────────┐
                │  Next.js Storefront      │ (TypeScript)
                │  - SSR/SSG               │
                │  - Apollo Client         │
                └───────────┬──────────────┘
                            │
                ┌───────────▼──────────────┐
                │  Federation Gateway      │ (TypeScript/NestJS)
                │  - Apollo Gateway        │
                │  - HMAC Signing          │
                │  - Request Routing       │
                └───────────┬──────────────┘
                            │
        ┌───────────────────┼───────────────────────┐
        │                   │                       │
        ▼                   ▼                       ▼
┌───────────────┐   ┌───────────────┐     ┌───────────────┐
│ Auth Service  │   │Catalog Service│ ... │ Cart Service  │
│ (Kotlin)      │   │ (Kotlin)      │     │ (Kotlin)      │
│ - PostgreSQL  │   │ - PostgreSQL  │     │ - Redis       │
└───────┬───────┘   └───────┬───────┘     └───────┬───────┘
        │                   │                       │
        └───────────────────┴───────────────────────┘
                            │
                    ┌───────▼────────┐
                    │     Kafka      │
                    │   (KRaft)      │
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │ Streams Service│ (Kotlin)
                    │ - Kafka Streams│
                    │ - RocksDB      │
                    └────────────────┘
```

### Service Responsibilities

| Service | Technology | Database | Purpose |
|---------|-----------|----------|---------|
| **Auth** | Kotlin/Spring Boot | PostgreSQL | User authentication, JWT issuance, password management |
| **Catalog** | Kotlin/Spring Boot | PostgreSQL | Product catalog, inventory, categories |
| **Order** | Kotlin/Spring Boot | PostgreSQL | Order management, order lifecycle |
| **Customer** | Kotlin/Spring Boot | PostgreSQL | Customer profiles, addresses, preferences |
| **Cart** | Kotlin/Spring Boot | Redis | Shopping cart with 30-day TTL |
| **Streams** | Kotlin/Kafka Streams | RocksDB | Real-time order analytics, event aggregation |
| **Gateway** | TypeScript/NestJS | - | GraphQL federation, request routing, HMAC signing |
| **Storefront** | TypeScript/Next.js | - | SSR frontend, user interface |

### Communication Patterns

#### 1. **Synchronous: GraphQL Federation**
- Client → Storefront → Gateway → Services
- Apollo Federation v2 with `@authenticated` and `@admin` directives
- HMAC-signed requests between gateway and subgraphs

#### 2. **Asynchronous: Event-Driven (Kafka)**
- Services publish domain events to Kafka topics
- Event naming: `dachshaus.{domain}.{event}` (e.g., `dachshaus.order.created`)
- Dead Letter Queue (DLQ): `dachshaus.dlq` with diagnostic headers
- Kafka Streams for real-time processing

#### 3. **Internal: REST (Gateway ↔ Auth)**
- Gateway calls Auth Service's `/auth/verify` endpoint for JWT validation

## 🔐 Security Architecture

### HMAC Request Signing

All requests from the gateway to backend services are signed with HMAC-SHA256:

```
X-Gateway-Signature: {timestamp}.{hmac}
```

**HMAC Payload**: `{x-user-id}:{x-user-roles}:{x-request-id}:{timestamp}`

Each service validates the signature using the shared secret from `services/common/` library.

### JWT Authentication

- **Algorithm**: RS256 (asymmetric keys)
- **Access Token**: 15 minutes
- **Refresh Token**: 7 days
- **Issuance**: Auth Service only
- **Validation**: Gateway validates, then propagates user context via headers

### Authorization

- `@authenticated`: Requires valid JWT
- `@admin`: Requires `ADMIN` role in JWT claims
- Role-based access control (RBAC) enforced at GraphQL directive level

## 🛠️ Technology Stack

### Backend Services (Kotlin)
- **Framework**: Spring Boot 3.x
- **GraphQL**: Netflix DGS (Domain Graph Service)
- **Database**: PostgreSQL 16 (Flyway migrations)
- **Cache**: Redis 7.2 (Cart service)
- **Messaging**: Apache Kafka 7.5 (KRaft mode)
- **Streaming**: Kafka Streams with RocksDB
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, Testcontainers
- **Runtime**: JRE 21

### Gateway (TypeScript)
- **Framework**: NestJS
- **GraphQL**: Apollo Federation Gateway
- **Runtime**: Node.js 20

### Storefront (TypeScript)
- **Framework**: Next.js 14 (App Router)
- **GraphQL Client**: Apollo Client
- **Runtime**: Node.js 20

### Infrastructure
- **Containers**: Docker multi-stage builds
- **Orchestration**: Kubernetes (Kustomize)
- **IaC**: Terraform (GCP)
- **CI/CD**: GitHub Actions
- **Package Management**: pnpm workspaces + Turborepo

## 📁 Project Structure

```
dachshaus/
├── .claude/packages/          # Work package specifications (PKG-01 to PKG-13)
├── services/                  # Kotlin microservices (Gradle multi-project)
│   ├── common/                # Shared library (security, Kafka, GraphQL directives)
│   ├── auth/                  # Authentication service
│   ├── catalog/               # Product catalog service
│   ├── order/                 # Order management service
│   ├── customer/              # Customer profile service
│   ├── cart/                  # Shopping cart service (Redis)
│   └── streams/               # Kafka Streams analytics
├── gateway/                   # NestJS Apollo Federation gateway
├── storefront/                # Next.js storefront frontend
├── packages/                  # Shared TypeScript packages
│   ├── tsconfig/              # Shared TypeScript configs
│   ├── eslint-config/         # Shared ESLint configs
│   └── graphql-schema/        # Shared GraphQL type definitions
├── infra/
│   ├── docker/                # Dockerfiles (kotlin-service, node-service)
│   └── terraform/             # GCP infrastructure as code
├── k8s/                       # Kubernetes manifests
│   ├── base/                  # Base Kustomize resources
│   ├── overlays/              # Environment-specific overlays
│   └── scripts/               # Deployment scripts
├── .github/workflows/         # CI/CD pipelines
├── docker-compose.yml         # Local development environment
├── Makefile                   # Common tasks orchestration
└── turbo.json                 # Turborepo configuration
```

## 🚀 Getting Started

### ⚡ One-Click Installation (Recommended)

The fastest way to get DachsHaus up and running is using our automated installer:

```bash
# Clone the repository
git clone https://github.com/vGsteiger/DachsHaus.git
cd DachsHaus

# Run the one-click installer
./install.sh
```

**That's it!** The installer will:
- ✓ Check system requirements automatically
- ✓ Generate secure credentials (HMAC secrets, JWT keys, passwords)
- ✓ Set up all services with health checks
- ✓ Configure production-ready settings
- ✓ Create backup and restore scripts
- ✓ Verify the installation

**After installation:**
- 🌐 **Storefront**: http://localhost:3000
- 🔧 **GraphQL Playground**: http://localhost:4000/graphql
- 📄 **Credentials**: See `CREDENTIALS.txt` for all passwords and access info

For detailed installation instructions, troubleshooting, and advanced configuration, see **[INSTALL.md](INSTALL.md)**.

---

### Prerequisites

- **Docker** (v20.10+) & **Docker Compose** (v2.0+)
- **Make** - For convenience commands
- **10GB+ free disk space**
- **4GB+ RAM** (8GB recommended)

### Manual Installation (Advanced)

If you prefer manual control over the installation:

1. **Clone the repository**
   ```bash
   git clone https://github.com/vGsteiger/DachsHaus.git
   cd DachsHaus
   ```

2. **Copy environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration (defaults work for local dev)
   ```

3. **Generate JWT keys**
   ```bash
   mkdir -p keys
   openssl genrsa -out keys/auth-private.pem 4096
   openssl rsa -in keys/auth-private.pem -pubout -out keys/auth-public.pem
   ```

4. **Start the entire stack**
   ```bash
   make dev
   # or
   docker-compose up
   ```

   This will start:
   - PostgreSQL (port 5432)
   - Redis (port 6379)
   - Kafka (port 9092)
   - All 7 Kotlin services (ports 8081-8085)
   - Gateway (port 4000)
   - Storefront (port 3000)

5. **Access the application**
   - **Storefront**: http://localhost:3000
   - **GraphQL Playground**: http://localhost:4000/graphql

### Development Setup (without Docker)

If you want to run services locally for development:

1. **Install dependencies**
   ```bash
   make install
   # or
   pnpm install
   ```

2. **Start infrastructure** (PostgreSQL, Redis, Kafka)
   ```bash
   docker-compose up postgres redis kafka
   ```

3. **Build Kotlin services**
   ```bash
   cd services
   ./gradlew build
   ```

4. **Run services** (in separate terminals or use your IDE)
   ```bash
   # Auth Service
   cd services/auth
   ./gradlew bootRun

   # Catalog Service
   cd services/catalog
   ./gradlew bootRun

   # ... repeat for other services
   ```

5. **Run Gateway**
   ```bash
   cd gateway
   pnpm dev
   ```

6. **Run Storefront**
   ```bash
   cd storefront
   pnpm dev
   ```

## 🧪 Testing

### Run all tests
```bash
make test
```

### Test Kotlin services only
```bash
cd services
./gradlew test
```

### Test TypeScript services only
```bash
pnpm turbo run test
```

### Integration tests
Kotlin services use Testcontainers for integration testing with real PostgreSQL, Redis, and Kafka instances.

## 🔨 Building

### Build all services
```bash
make build
```

### Build Docker images
```bash
make docker
```

### Build specific service
```bash
# Kotlin service
cd services/{service-name}
./gradlew build

# TypeScript service
cd {gateway|storefront}
pnpm build
```

## 🐳 Docker Deployment

### Using docker-compose
```bash
docker-compose up --build
```

### Building individual images
```bash
# Kotlin services
docker build -f infra/docker/kotlin-service.Dockerfile \
  --build-arg SERVICE_NAME=auth \
  -t dachshaus/auth .

# TypeScript services
docker build -f infra/docker/node-service.Dockerfile \
  --build-arg SERVICE_NAME=gateway \
  -t dachshaus/gateway .
```

## ☸️ Kubernetes Deployment

### Prerequisites
- Kubernetes cluster (GKE, EKS, AKS, or local like Minikube)
- `kubectl` configured
- `kustomize` (or use `kubectl` with `-k` flag)

### Deploy to Kubernetes
```bash
# Development overlay
kubectl apply -k k8s/overlays/dev

# Production overlay
kubectl apply -k k8s/overlays/prod
```

### Using Terraform for GCP
```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

## 📦 Implementation Phases

The project is organized into 13 work packages (see `.claude/packages/`):

### Phase 1 (Foundation)
- **PKG-01**: Monorepo scaffold
- **PKG-12**: Terraform infrastructure

### Phase 2 (Core Infrastructure)
- **PKG-02**: Common Kotlin library
- **PKG-03**: Federation Gateway
- **PKG-10**: Storefront shell

### Phase 3 (Core Services)
- **PKG-04**: Auth Service
- **PKG-05**: Catalog Service
- **PKG-07**: Customer Service
- **PKG-08**: Cart Service

### Phase 4 (Advanced Features)
- **PKG-06**: Order Service
- **PKG-09**: Kafka Streams analytics

### Phase 5 (Production Readiness)
- **PKG-11**: Kubernetes manifests
- **PKG-13**: CI/CD pipelines
- **PKG-10**: Complete Storefront

## 🧰 Common Tasks

### Clean build artifacts
```bash
make clean
```

### View logs (Docker)
```bash
docker-compose logs -f {service-name}
```

### Restart a service
```bash
docker-compose restart {service-name}
```

### Access Kafka
```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Access PostgreSQL
```bash
docker-compose exec postgres psql -U dachshaus -d dachshaus
```

### Access Redis CLI
```bash
docker-compose exec redis redis-cli
```

## 🎓 Learning Goals

This project demonstrates:

1. **Polyglot Microservices**: Multiple languages working together
2. **GraphQL Federation**: Distributed schema composition
3. **Event-Driven Architecture**: Kafka for async communication
4. **CQRS & Event Sourcing**: Separation of reads and writes
5. **Security**: HMAC signing, JWT, RBAC
6. **Cloud-Native**: Containerization, orchestration, IaC
7. **Modern DevOps**: CI/CD, automated testing, infrastructure as code
8. **Domain-Driven Design**: Bounded contexts, domain events
9. **Database per Service**: Service isolation and autonomy
10. **API Gateway Pattern**: Single entry point with federation

## 📝 Key Design Decisions

### Why Kotlin for Backend?
- Type safety with concise syntax
- Excellent Spring Boot integration
- Coroutines for async programming
- Strong ecosystem for JVM

### Why GraphQL Federation?
- Single graph API for clients
- Service autonomy (each owns its schema)
- Type-safe queries
- Reduced over-fetching

### Why Kafka?
- High throughput event streaming
- Exactly-once semantics with Kafka Streams
- Persistent event log
- Decoupling between services

### Why HMAC Signing?
- Prevents request tampering
- Validates request integrity
- Protects against replay attacks (with timestamp)
- Shared secret model for internal services

### Why Redis for Cart?
- Fast in-memory operations
- Built-in TTL for ephemeral data
- Simple data model (hash per user)
- Reduces database load

## 🤝 Contributing

This is a learning/demo project. Feel free to fork and experiment! If you find issues or have suggestions, open an issue or PR.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Netflix DGS](https://netflix.github.io/dgs/)
- [Apollo Federation](https://www.apollographql.com/docs/federation/)
- [Next.js](https://nextjs.org/)
- [Apache Kafka](https://kafka.apache.org/)
- [Kubernetes](https://kubernetes.io/)
- [Terraform](https://www.terraform.io/)

---

**Happy exploring! 🦡**
