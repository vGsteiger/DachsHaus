# Architecture Extension: One-Click Installation System

## Overview

This document describes the architectural changes made to enable a safe, one-click installation of the DachsHaus e-commerce platform.

## Problem Statement

The original DachsHaus platform required manual setup involving:
- Manual environment configuration
- Manual secret generation
- Manual JWT key creation
- Understanding of Docker, Kubernetes, and microservices
- Complex multi-step installation process
- No validation or health checks
- Manual security hardening

**Goal**: Transform this into a single-command installation that is:
- ✓ Safe and secure by default
- ✓ Production-ready
- ✓ Self-validating
- ✓ User-friendly
- ✓ Recoverable from failures

## Solution Architecture

### 1. Automated Installation Script (`install.sh`)

**Design Decisions**:

- **Bash-based**: Maximum compatibility across Linux/macOS systems
- **Idempotent**: Can be run multiple times safely
- **Progressive**: Step-by-step with clear progress indicators
- **Fail-safe**: Automatic rollback on errors
- **Logged**: Complete audit trail in `install.log`

**Key Features**:

```bash
./install.sh
```

Executes 10 automated steps:
1. System requirements validation (Docker, Compose, disk space, memory)
2. Secure credential generation (HMAC, JWT, passwords)
3. Environment file creation (.env)
4. Production Docker Compose configuration
5. Service orchestration with health checks
6. Infrastructure validation
7. Backup script creation
8. Utility script generation
9. Post-installation verification
10. User-facing summary and instructions

### 2. Security Architecture

#### Automatic Credential Generation

**HMAC Secrets** (Gateway ↔ Services):
```bash
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```
- 256-bit random secret
- Used for request signature verification
- Prevents request tampering and replay attacks

**JWT Keys** (Authentication):
```bash
openssl genrsa -out auth-private.pem 4096
openssl rsa -in auth-private.pem -pubout -out auth-public.pem
```
- 4096-bit RSA keypair
- RS256 algorithm for JWT signing
- Private key with 600 permissions

**Database Passwords**:
```bash
openssl rand -base64 24
```
- 192-bit random passwords
- Unique per installation
- Never stored in code

#### Credentials Storage

```
CREDENTIALS.txt         # User-facing summary (chmod 600)
.env                    # Service configuration (chmod 600)
keys/auth-private.pem   # JWT private key (chmod 600)
keys/auth-public.pem    # JWT public key (chmod 644)
```

All files automatically added to `.gitignore` to prevent accidental commits.

### 3. Health Check Architecture

#### Infrastructure Health Checks

**PostgreSQL**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U dachshaus"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

**Redis**:
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
  interval: 10s
  timeout: 3s
  retries: 5
```

**Kafka**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

#### Service Health Checks

All microservices expose `/healthz` endpoints:
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 90s
```

### 4. Docker Compose Architecture

#### Network Segmentation

```yaml
networks:
  public:   # Storefront, Gateway (exposed to internet)
  internal: # Services, databases (isolated)
```

**Security Benefits**:
- Services never directly exposed to internet
- Client → Storefront → Gateway → Services
- Databases only accessible from internal network
- Defense in depth architecture

#### Production Configuration (`docker-compose.prod.yml`)

**Restart Policies**:
```yaml
restart: unless-stopped
```
Ensures services automatically recover from failures.

**Resource Limits** (prevents resource exhaustion):
```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 2G
```

**Enhanced Logging**:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

### 5. Backup & Restore Architecture

#### Automated Backup Script

```bash
./scripts/backup.sh
```

Creates:
```
backups/postgres/backup_YYYYMMDD_HHMMSS.sql.gz
```

Features:
- Timestamped backups
- Compressed (gzip)
- Non-blocking (runs while services are up)
- Can be scheduled with cron

#### Restore Script

```bash
./scripts/restore.sh backup_20260309_120000.sql.gz
```

Features:
- Validates backup file exists
- Decompresses and restores
- Idempotent (can be run multiple times)

### 6. Make-based Interface

Enhanced Makefile provides unified interface:

```bash
make setup      # Run installation
make dev        # Start development environment
make status     # Check service health
make backup     # Create backup
make restore    # Restore from backup
make check      # System diagnostics
```

Benefits:
- Consistent interface across environments
- Self-documenting (`make help`)
- Easy to extend
- CI/CD friendly

### 7. Documentation Architecture

#### INSTALL.md - Complete Installation Guide
- Detailed installation instructions
- Troubleshooting section
- Advanced configuration
- Production deployment guides

#### QUICKSTART.md - 5-Minute Guide
- Minimal steps to get running
- First-time user flow
- Common tasks
- API examples

#### SECURITY.md - Security Hardening
- Pre-production checklist
- Security best practices
- Compliance guides (GDPR, PCI DSS)
- Incident response procedures

### 8. Validation Architecture

#### Pre-flight Checks

```bash
check_requirements() {
  # Docker installed and running
  # Docker Compose available
  # Sufficient disk space (10GB+)
  # Sufficient memory (4GB+)
  # Make installed
}
```

#### Post-installation Validation

```bash
verify_installation() {
  # Environment file exists
  # JWT keys exist
  # Services are running
  # Health checks passing
}
```

## Changes to Existing Architecture

### No Breaking Changes

The one-click installer is **additive** - it doesn't modify existing architecture:

- ✓ Existing manual installation still works
- ✓ Docker Compose files unchanged
- ✓ Service configurations unchanged
- ✓ Kubernetes manifests unchanged

### New Files Added

```
install.sh                  # Main installation script
INSTALL.md                  # Installation documentation
QUICKSTART.md               # Quick start guide
SECURITY.md                 # Security checklist
scripts/backup.sh           # Backup automation
scripts/restore.sh          # Restore automation
```

### Modified Files

```
.gitignore                  # Added installation artifacts
Makefile                    # Added convenience commands
README.md                   # Highlighted one-click install
```

## Benefits of New Architecture

### For Users
1. **Simplicity**: One command to get started
2. **Security**: Secure by default, no manual configuration
3. **Reliability**: Self-validating with health checks
4. **Recoverability**: Automatic backups and rollback
5. **Transparency**: Clear progress reporting and logs

### For Developers
1. **Faster onboarding**: New developers productive in 5 minutes
2. **Consistent environments**: Everyone has the same setup
3. **Easy testing**: Spin up/down environments quickly
4. **CI/CD ready**: Automated scripts for pipelines

### For Operations
1. **Production-ready**: Secure defaults and best practices
2. **Monitoring**: Built-in health checks
3. **Backup/Restore**: Automated data protection
4. **Disaster recovery**: Quick recovery from failures

## Security Considerations

### What's Generated Automatically

✓ **HMAC secrets**: 256-bit random keys
✓ **JWT keys**: 4096-bit RSA keypairs
✓ **Database passwords**: 192-bit random passwords
✓ **Redis passwords**: Secure authentication
✓ **File permissions**: Restrictive (600 for secrets)

### What's NOT in Git

✗ `.env` files
✗ `CREDENTIALS.txt`
✗ `keys/*.pem`
✗ `backups/`
✗ `install.log`
✗ `docker-compose.prod.yml` (contains secrets)

All automatically added to `.gitignore`.

### Defense in Depth

1. **Network**: Public/internal segmentation
2. **Authentication**: JWT with short expiration
3. **Authorization**: RBAC with GraphQL directives
4. **Transport**: HMAC request signing
5. **Data**: Encrypted at rest and in transit
6. **Monitoring**: Health checks and logging

## Compatibility

### Supported Platforms
- ✓ Linux (Ubuntu, Debian, CentOS, RHEL)
- ✓ macOS (Intel and Apple Silicon)
- ✓ Windows (via WSL2)

### Requirements
- Docker 20.10+
- Docker Compose 2.0+
- 10GB disk space
- 4GB RAM (8GB recommended)
- Bash 4.0+
- OpenSSL (for key generation)

## Future Enhancements

### Planned Improvements

1. **Interactive Configuration**
   - Wizard for custom configuration
   - Environment selection (dev/staging/prod)
   - Feature toggles

2. **Cloud Provider Support**
   - GCP one-click deployment
   - AWS one-click deployment
   - Azure one-click deployment

3. **Monitoring Integration**
   - Prometheus metrics
   - Grafana dashboards
   - Alert configuration

4. **Advanced Security**
   - Vault integration for secrets
   - Automated secret rotation
   - Security scanning

5. **Testing**
   - Automated smoke tests
   - Load testing tools
   - Security testing

## Migration Guide

### From Manual Setup to One-Click

If you already have a manual installation:

1. **Backup your data**:
   ```bash
   docker-compose exec postgres pg_dump -U dachshaus dachshaus > backup.sql
   ```

2. **Note your configuration**:
   - Copy your `.env` file
   - Backup your JWT keys

3. **Stop existing services**:
   ```bash
   docker-compose down -v
   ```

4. **Run installer**:
   ```bash
   ./install.sh
   ```

5. **Restore data** (optional):
   ```bash
   cat backup.sql | docker-compose exec -T postgres psql -U dachshaus dachshaus
   ```

## Conclusion

The one-click installation system transforms DachsHaus from a complex microservices platform into a user-friendly e-commerce solution that can be safely deployed in minutes.

**Key Achievements**:
- ✓ Reduced installation time from hours to minutes
- ✓ Eliminated manual configuration errors
- ✓ Improved security with automatic credential generation
- ✓ Added production-ready defaults
- ✓ Provided comprehensive documentation
- ✓ Enabled easy backup and recovery
- ✓ Made the platform accessible to non-technical users

The architecture remains flexible and extensible, supporting both simple one-click installations and advanced custom deployments.

---

**For questions or issues, see:**
- Installation Guide: `INSTALL.md`
- Quick Start: `QUICKSTART.md`
- Security: `SECURITY.md`
- Main Documentation: `README.md`
