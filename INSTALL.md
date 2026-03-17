# DachsHaus One-Click Installation Guide

Welcome to DachsHaus! This guide will help you set up a complete e-commerce platform in minutes.

## 🚀 Quick Start (One-Click Install)

The easiest way to get started is using our automated installation script:

```bash
./install.sh
```

That's it! The script will:
- ✓ Check system requirements
- ✓ Generate secure credentials automatically
- ✓ Set up all services (databases, microservices, frontend)
- ✓ Configure networking and health checks
- ✓ Create backup scripts
- ✓ Verify the installation

## 📋 Prerequisites

Before running the installer, ensure you have:

- **Docker** (v20.10+) - [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose** (v2.0+) - Usually included with Docker Desktop
- **Make** - For running convenience commands
- **10GB+ free disk space**
- **4GB+ RAM** (8GB recommended)

### Quick Dependency Check

```bash
# Check if you have everything
docker --version
docker-compose --version
make --version
```

## 🔧 Installation Options

### Option 1: Automated One-Click Install (Recommended)

```bash
# Clone the repository
git clone https://github.com/vGsteiger/DachsHaus.git
cd DachsHaus

# Run the installer
./install.sh
```

The installer will:
1. Verify system requirements
2. Generate secure credentials (HMAC secrets, JWT keys, database passwords)
3. Create production-ready Docker Compose configuration
4. Start all services with health checks
5. Create backup and restore scripts
6. Provide you with access URLs and credentials

### Option 2: Manual Installation

If you prefer manual control:

```bash
# 1. Copy and customize environment
cp .env.example .env
# Edit .env with your preferred settings

# 2. Generate JWT keys manually
mkdir -p keys
openssl genrsa -out keys/auth-private.pem 4096
openssl rsa -in keys/auth-private.pem -pubout -out keys/auth-public.pem

# 3. Start services
docker-compose up -d

# 4. Check status
docker-compose ps
```

## 🎯 What Gets Installed

The one-click installer sets up a complete e-commerce platform:

### Infrastructure Services
- **PostgreSQL 16** - Main database for Auth, Catalog, Order, and Customer services
- **Redis 7.2** - In-memory cache for Cart service
- **Kafka 7.5** - Event streaming (KRaft mode, no Zookeeper)

### Backend Microservices (Kotlin/Spring Boot)
- **Auth Service** (port 8081) - User authentication, JWT issuance
- **Catalog Service** (port 8082) - Product catalog, inventory
- **Order Service** (port 8083) - Order management, checkout
- **Customer Service** (port 8084) - Customer profiles, addresses
- **Cart Service** (port 8085) - Shopping cart (Redis-backed)
- **Streams Service** - Real-time event processing (Kafka Streams)

### Gateway & Frontend (TypeScript)
- **Gateway** (port 4000) - Apollo Federation gateway, GraphQL API
- **Storefront** (port 3000) - Next.js frontend, SSR/SSG

## 🔐 Security Features

The installer automatically configures production-grade security:

### Automatic Credential Generation
- **HMAC secrets** - 256-bit random keys for request signing
- **JWT keys** - 4096-bit RSA keypairs for token signing
- **Database passwords** - Cryptographically secure random passwords
- **Redis passwords** - Secure authentication for cache layer

### Security Best Practices
- ✓ No default passwords in code
- ✓ All secrets generated uniquely per installation
- ✓ Private keys with restrictive permissions (600)
- ✓ Credentials stored in secure files (not committed to git)
- ✓ Network isolation (public/internal Docker networks)
- ✓ HMAC request signing between gateway and services
- ✓ JWT authentication with short-lived access tokens (15min)

### Credentials Storage

After installation, your credentials are saved in:
- **CREDENTIALS.txt** - All passwords and access information
- **.env** - Environment configuration
- **keys/** - JWT private/public keys

⚠️ **Important**: These files contain sensitive information. Never commit them to version control!

## 🏥 Health Checks & Monitoring

The installer configures comprehensive health checks:

### Service Health Checks
- PostgreSQL: Connection readiness check every 10s
- Redis: Ping check every 10s
- Kafka: Broker API check every 30s
- Services: HTTP health endpoints every 30s

### Monitoring Commands

```bash
# Check all services status
docker-compose ps

# View real-time logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f gateway

# Check health of a specific service
docker-compose exec gateway wget -qO- http://localhost:4000/healthz
```

## 🔄 Backup & Restore

The installer creates backup scripts automatically:

### Backup Database

```bash
./scripts/backup.sh
```

Creates timestamped backup in `./backups/postgres/backup_YYYYMMDD_HHMMSS.sql.gz`

### Restore Database

```bash
./scripts/restore.sh ./backups/postgres/backup_20260309_120000.sql.gz
```

### Backup Schedule (Recommended)

Set up automated backups with cron:

```bash
# Add to crontab (runs daily at 2 AM)
0 2 * * * cd /path/to/DachsHaus && ./scripts/backup.sh
```

## 🛠️ Common Tasks

### Start the Platform

```bash
# Using Make
make dev

# Or directly
docker-compose up -d
```

### Stop the Platform

```bash
docker-compose down
```

### Stop and Remove All Data

```bash
# ⚠️ WARNING: This deletes all data!
docker-compose down -v
```

### Rebuild Services

```bash
# Rebuild all services
docker-compose up -d --build

# Rebuild specific service
docker-compose up -d --build gateway
```

### View Service Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f auth

# Last 100 lines
docker-compose logs --tail=100 gateway
```

### Execute Commands in Containers

```bash
# PostgreSQL
docker-compose exec postgres psql -U dachshaus

# Redis
docker-compose exec redis redis-cli

# Kafka topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 🧪 Testing the Installation

### Quick Verification

```bash
# Check if storefront is accessible
curl http://localhost:3000

# Check GraphQL endpoint
curl http://localhost:4000/graphql

# Check gateway health
curl http://localhost:4000/healthz
```

### Run Tests

```bash
# All tests
make test

# Kotlin services only
cd services && ./gradlew test

# TypeScript services only
pnpm turbo run test
```

## 🔍 Troubleshooting

### Services Won't Start

```bash
# Check Docker daemon
docker info

# Check disk space
df -h

# Check memory
free -h

# View detailed error logs
docker-compose logs auth
```

### Port Conflicts

If you get port binding errors:

```bash
# Check what's using the port
sudo lsof -i :3000  # Replace 3000 with your port

# Either stop the conflicting service or edit docker-compose.yml
# to use different ports
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker-compose exec postgres psql -U dachshaus -d dachshaus -c "SELECT 1;"

# Check environment variables
docker-compose exec auth env | grep DATABASE
```

### Kafka Issues

```bash
# Check Kafka logs
docker-compose logs kafka

# List topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer groups
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Reset Everything

If things go wrong and you want to start fresh:

```bash
# Stop and remove everything
docker-compose down -v

# Clean up images (optional)
docker-compose down --rmi all

# Remove generated files
rm -rf keys/ .env CREDENTIALS.txt

# Re-run installer
./install.sh
```

## 📊 Accessing the Platform

After successful installation:

### User Interfaces
- **Storefront**: http://localhost:3000
  - Browse products, add to cart, checkout
  - User registration and login
  - Order tracking with live updates

- **GraphQL Playground**: http://localhost:4000/graphql
  - Interactive API exploration
  - Test queries and mutations
  - View schema documentation

### Default Admin Account

On first run, a default admin account is created:
- **Email**: admin@dachshaus.local
- **Password**: admin123

⚠️ **Change this password immediately after first login!**

### Creating Your First Products

```graphql
# Login as admin first to get token
mutation Login {
  login(email: "admin@dachshaus.local", password: "admin123") {
    accessToken
    user { id email }
  }
}

# Then create a product (include Authorization header)
mutation CreateProduct {
  createProduct(input: {
    name: "Sample Product"
    description: "A great product"
    priceCents: 2999
    sku: "SAMPLE-001"
  }) {
    id
    name
    priceCents
  }
}
```

## 🚀 Production Deployment

The one-click installer is perfect for development, but for production:

### Use Production Compose File

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Production configuration includes:
- Automatic container restarts
- Enhanced health checks
- Resource limits
- Logging configuration
- Security hardening

### Deploy to Kubernetes

For production-grade deployment:

```bash
# Deploy to dev environment
kubectl apply -k k8s/overlays/dev

# Deploy to production
kubectl apply -k k8s/overlays/prod
```

See `CLAUDE.md` for detailed Kubernetes deployment instructions.

### Deploy to GCP

```bash
cd infra/terraform
terraform init
terraform plan -var-file=environments/prod.tfvars
terraform apply -var-file=environments/prod.tfvars
```

## 📚 Next Steps

After installation:

1. **Change Default Passwords**
   - Admin account password
   - Database passwords (if deploying to production)

2. **Configure Email/SMS** (for notifications)
   - Update notification service configuration
   - Set up SMTP or email provider

3. **Set Up Monitoring**
   - Configure alerting for production
   - Set up log aggregation
   - Enable metrics collection

4. **Customize Storefront**
   - Update branding and styling
   - Configure payment providers
   - Set up shipping methods

5. **Load Sample Data** (optional)
   - Create product catalog
   - Set up categories and collections
   - Add test customers and orders

## 🆘 Getting Help

If you encounter issues:

1. **Check the logs**: `docker-compose logs -f`
2. **Review CLAUDE.md**: Detailed architecture documentation
3. **Check installation log**: `cat install.log`
4. **Open an issue**: https://github.com/vGsteiger/DachsHaus/issues

## 📝 Uninstalling

To completely remove DachsHaus:

```bash
# Stop and remove containers
docker-compose down -v

# Remove generated files
rm -rf keys/ .env CREDENTIALS.txt backups/

# Remove Docker images (optional)
docker rmi $(docker images 'dachshaus/*' -q)
```

## 🔒 Security Checklist

Before going to production:

- [ ] Change all default passwords
- [ ] Rotate generated secrets
- [ ] Set up SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Enable audit logging
- [ ] Set up automated backups
- [ ] Configure monitoring and alerts
- [ ] Review and update .env for production
- [ ] Implement rate limiting
- [ ] Set up DDoS protection

---

**Happy shopping! 🦡🏠**

For more information, see [README.md](README.md) and [CLAUDE.md](CLAUDE.md).
