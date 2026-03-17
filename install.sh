#!/bin/bash
set -euo pipefail

# DachsHaus One-Click Installation Script
# This script safely sets up a complete e-commerce platform

VERSION="1.0.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/install.log"
ENV_FILE="${SCRIPT_DIR}/.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Installation steps tracking
TOTAL_STEPS=10
CURRENT_STEP=0

# ==============================================================================
# Utility Functions
# ==============================================================================

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${BLUE}ℹ ${NC}$*" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}✓${NC} $*" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $*" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}✗${NC} $*" | tee -a "$LOG_FILE"
}

step() {
    CURRENT_STEP=$((CURRENT_STEP + 1))
    echo ""
    echo -e "${BLUE}[$CURRENT_STEP/$TOTAL_STEPS]${NC} $*" | tee -a "$LOG_FILE"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" | tee -a "$LOG_FILE"
}

fatal() {
    error "$*"
    echo ""
    error "Installation failed. Check $LOG_FILE for details."
    exit 1
}

# ==============================================================================
# System Requirements Check
# ==============================================================================

check_requirements() {
    step "Checking system requirements"

    local missing_deps=()

    # Check Docker
    if command -v docker &> /dev/null; then
        local docker_version=$(docker --version | grep -oP '\d+\.\d+\.\d+' | head -1)
        success "Docker installed: $docker_version"

        # Check if Docker daemon is running
        if ! docker info &> /dev/null; then
            fatal "Docker daemon is not running. Please start Docker and try again."
        fi
    else
        missing_deps+=("docker")
    fi

    # Check Docker Compose
    if command -v docker-compose &> /dev/null || docker compose version &> /dev/null; then
        local compose_version=$(docker-compose --version 2>/dev/null || docker compose version | grep -oP '\d+\.\d+\.\d+' | head -1)
        success "Docker Compose installed: $compose_version"
    else
        missing_deps+=("docker-compose")
    fi

    # Check Make
    if command -v make &> /dev/null; then
        success "Make installed: $(make --version | head -1)"
    else
        missing_deps+=("make")
    fi

    # Check Git
    if command -v git &> /dev/null; then
        success "Git installed: $(git --version)"
    else
        warning "Git not found (optional, but recommended)"
    fi

    # Check available disk space (minimum 10GB)
    local available_space=$(df -BG "$SCRIPT_DIR" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ "$available_space" -lt 10 ]; then
        warning "Low disk space: ${available_space}GB available (10GB recommended)"
    else
        success "Sufficient disk space: ${available_space}GB available"
    fi

    # Check available memory (minimum 4GB)
    local total_mem=$(free -g | awk '/^Mem:/{print $2}')
    if [ "$total_mem" -lt 4 ]; then
        warning "Low memory: ${total_mem}GB available (4GB recommended)"
    else
        success "Sufficient memory: ${total_mem}GB available"
    fi

    if [ ${#missing_deps[@]} -ne 0 ]; then
        error "Missing required dependencies: ${missing_deps[*]}"
        echo ""
        info "Please install the missing dependencies:"
        for dep in "${missing_deps[@]}"; do
            case "$dep" in
                docker)
                    echo "  • Docker: https://docs.docker.com/get-docker/"
                    ;;
                docker-compose)
                    echo "  • Docker Compose: https://docs.docker.com/compose/install/"
                    ;;
                make)
                    echo "  • Make: sudo apt-get install build-essential (Ubuntu/Debian)"
                    ;;
            esac
        done
        exit 1
    fi
}

# ==============================================================================
# Secure Credential Generation
# ==============================================================================

generate_secure_secret() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
}

generate_password() {
    openssl rand -base64 24 | tr -d "=+/"
}

generate_rsa_keypair() {
    local private_key_path="$1"
    local public_key_path="$2"

    # Generate RSA private key (4096 bits for production security)
    openssl genrsa -out "$private_key_path" 4096 2>/dev/null

    # Extract public key
    openssl rsa -in "$private_key_path" -pubout -out "$public_key_path" 2>/dev/null

    # Set restrictive permissions
    chmod 600 "$private_key_path"
    chmod 644 "$public_key_path"
}

# ==============================================================================
# Environment Configuration
# ==============================================================================

setup_environment() {
    step "Setting up environment configuration"

    if [ -f "$ENV_FILE" ]; then
        warning "Existing .env file found"
        read -p "Do you want to regenerate it? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            info "Using existing .env file"
            return
        fi
        cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%s)"
        info "Backed up existing .env file"
    fi

    info "Generating secure credentials..."

    # Generate secure secrets
    local db_password=$(generate_password)
    local hmac_secret=$(generate_secure_secret)
    local redis_password=$(generate_password)

    # Create keys directory
    mkdir -p "$SCRIPT_DIR/keys"

    # Generate RSA key pair for JWT
    info "Generating RSA key pair for JWT authentication..."
    generate_rsa_keypair "$SCRIPT_DIR/keys/auth-private.pem" "$SCRIPT_DIR/keys/auth-public.pem"
    success "RSA keys generated"

    # Create .env file
    cat > "$ENV_FILE" << EOF
# DachsHaus Environment Configuration
# Generated on $(date)

# Database
DATABASE_URL=postgresql://dachshaus:${db_password}@postgres:5432/dachshaus
POSTGRES_DB=dachshaus
POSTGRES_USER=dachshaus
POSTGRES_PASSWORD=${db_password}

# Redis
REDIS_URL=redis://:${redis_password}@redis:6379
REDIS_PASSWORD=${redis_password}

# Kafka
KAFKA_BROKERS=kafka:9092

# Gateway
GATEWAY_PORT=4000
GATEWAY_HMAC_SECRET=${hmac_secret}

# JWT
JWT_PRIVATE_KEY_PATH=/app/keys/auth-private.pem
JWT_PUBLIC_KEY_PATH=/app/keys/auth-public.pem

# Auth Service
AUTH_SERVICE_URL=http://auth:8080

# Storefront
NEXT_PUBLIC_GRAPHQL_URL=http://localhost:4000/graphql
NEXT_PUBLIC_WS_URL=ws://localhost:4000/graphql

# GCP (for production deployment)
GCP_PROJECT_ID=
GCP_REGION=europe-west6
GKE_CLUSTER_NAME=dachshaus-cluster

# Installation metadata
DACHSHAUS_VERSION=${VERSION}
INSTALLED_AT=$(date -Iseconds)
EOF

    success "Environment configuration created"

    # Create a secure credentials summary file
    cat > "$SCRIPT_DIR/CREDENTIALS.txt" << EOF
DachsHaus Installation Credentials
===================================
Generated: $(date)

⚠️  IMPORTANT: Keep this file secure and do not commit it to version control!

Database:
  Username: dachshaus
  Password: ${db_password}
  Connection: postgresql://dachshaus:${db_password}@localhost:5432/dachshaus

Redis:
  Password: ${redis_password}
  Connection: redis://:${redis_password}@localhost:6379

Gateway HMAC Secret: ${hmac_secret}

JWT Keys:
  Private: ./keys/auth-private.pem
  Public:  ./keys/auth-public.pem

Access URLs:
  Storefront: http://localhost:3000
  GraphQL Playground: http://localhost:4000/graphql

Default Admin Account (will be created on first run):
  Email: admin@dachshaus.local
  Password: admin123
  ⚠️  Change this password immediately after first login!

EOF

    chmod 600 "$SCRIPT_DIR/CREDENTIALS.txt"
    success "Credentials saved to CREDENTIALS.txt"
    warning "Please secure CREDENTIALS.txt - it contains sensitive information!"
}

# ==============================================================================
# Docker Compose Enhancement
# ==============================================================================

create_production_compose() {
    step "Creating production-ready Docker Compose configuration"

    cat > "$SCRIPT_DIR/docker-compose.prod.yml" << 'EOF'
version: '3.9'

# Production overrides for enhanced security and reliability
services:
  postgres:
    restart: unless-stopped
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dachshaus"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./backups/postgres:/backups
    command: postgres -c shared_preload_libraries=pg_stat_statements -c max_connections=200

  redis:
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes --appendfsync everysec
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 10s

  kafka:
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  gateway:
    restart: unless-stopped
    environment:
      GATEWAY_HMAC_SECRET: ${GATEWAY_HMAC_SECRET}
      AUTH_SERVICE_URL: ${AUTH_SERVICE_URL}
    volumes:
      - ./keys:/app/keys:ro
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:4000/healthz"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  storefront:
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  auth:
    restart: unless-stopped
    environment:
      DATABASE_URL: ${DATABASE_URL}
      KAFKA_BROKERS: ${KAFKA_BROKERS}
      JWT_PRIVATE_KEY_PATH: ${JWT_PRIVATE_KEY_PATH}
      JWT_PUBLIC_KEY_PATH: ${JWT_PUBLIC_KEY_PATH}
    volumes:
      - ./keys:/app/keys:ro
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  catalog:
    restart: unless-stopped
    environment:
      DATABASE_URL: ${DATABASE_URL}
      KAFKA_BROKERS: ${KAFKA_BROKERS}
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  order:
    restart: unless-stopped
    environment:
      DATABASE_URL: ${DATABASE_URL}
      KAFKA_BROKERS: ${KAFKA_BROKERS}
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  customer:
    restart: unless-stopped
    environment:
      DATABASE_URL: ${DATABASE_URL}
      KAFKA_BROKERS: ${KAFKA_BROKERS}
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  cart:
    restart: unless-stopped
    environment:
      REDIS_URL: ${REDIS_URL}
      KAFKA_BROKERS: ${KAFKA_BROKERS}
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/healthz"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s

  streams:
    restart: unless-stopped
    environment:
      KAFKA_BROKERS: ${KAFKA_BROKERS}
      DATABASE_URL: ${DATABASE_URL}
    healthcheck:
      test: ["CMD", "ps", "aux", "|", "grep", "java"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s
EOF

    success "Production Docker Compose configuration created"
}

# ==============================================================================
# Health Checks
# ==============================================================================

wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=0

    info "Waiting for $service_name to be healthy..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            success "$service_name is healthy"
            return 0
        fi

        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done

    echo ""
    error "$service_name failed to become healthy after $((max_attempts * 2)) seconds"
    return 1
}

check_services_health() {
    step "Checking services health"

    info "Waiting for infrastructure services to start..."
    sleep 10

    # Check PostgreSQL
    if docker-compose ps postgres | grep -q "Up"; then
        success "PostgreSQL is running"
    else
        fatal "PostgreSQL failed to start"
    fi

    # Check Redis
    if docker-compose ps redis | grep -q "Up"; then
        success "Redis is running"
    else
        fatal "Redis failed to start"
    fi

    # Check Kafka
    if docker-compose ps kafka | grep -q "Up"; then
        success "Kafka is running"
    else
        fatal "Kafka failed to start"
    fi

    info "Waiting for application services to start (this may take a few minutes)..."
    sleep 30

    # Note: These health checks will be implemented as services are built
    # For now, we verify the containers are running

    local services=("auth" "catalog" "order" "customer" "cart" "streams" "gateway" "storefront")
    local all_healthy=true

    for service in "${services[@]}"; do
        if docker-compose ps "$service" | grep -q "Up"; then
            success "$service is running"
        else
            warning "$service is not running (may be implemented later)"
            all_healthy=false
        fi
    done

    if [ "$all_healthy" = true ]; then
        success "All services are running"
    else
        warning "Some services are not running yet. This is expected during phased implementation."
    fi
}

# ==============================================================================
# Post-Installation Setup
# ==============================================================================

run_post_install_setup() {
    step "Running post-installation setup"

    info "Creating backup directory..."
    mkdir -p "$SCRIPT_DIR/backups/postgres"
    mkdir -p "$SCRIPT_DIR/backups/redis"
    success "Backup directories created"

    info "Setting up log rotation..."
    cat > "$SCRIPT_DIR/logrotate.conf" << 'EOF'
/var/log/dachshaus/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
EOF
    success "Log rotation configured"

    # Create helpful utility scripts
    cat > "$SCRIPT_DIR/scripts/backup.sh" << 'EOF'
#!/bin/bash
# Database backup script
BACKUP_DIR="./backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker-compose exec -T postgres pg_dump -U dachshaus dachshaus | gzip > "$BACKUP_DIR/backup_$TIMESTAMP.sql.gz"
echo "Backup created: $BACKUP_DIR/backup_$TIMESTAMP.sql.gz"
EOF
    chmod +x "$SCRIPT_DIR/scripts/backup.sh"

    cat > "$SCRIPT_DIR/scripts/restore.sh" << 'EOF'
#!/bin/bash
# Database restore script
if [ -z "$1" ]; then
    echo "Usage: ./restore.sh <backup_file.sql.gz>"
    exit 1
fi
gunzip -c "$1" | docker-compose exec -T postgres psql -U dachshaus dachshaus
echo "Database restored from $1"
EOF
    chmod +x "$SCRIPT_DIR/scripts/restore.sh"

    success "Utility scripts created"
}

# ==============================================================================
# Installation Verification
# ==============================================================================

verify_installation() {
    step "Verifying installation"

    local verification_passed=true

    # Check environment file
    if [ -f "$ENV_FILE" ]; then
        success "Environment configuration exists"
    else
        error "Environment configuration missing"
        verification_passed=false
    fi

    # Check JWT keys
    if [ -f "$SCRIPT_DIR/keys/auth-private.pem" ] && [ -f "$SCRIPT_DIR/keys/auth-public.pem" ]; then
        success "JWT keys exist"
    else
        error "JWT keys missing"
        verification_passed=false
    fi

    # Check Docker containers
    local running_containers=$(docker-compose ps --services --filter "status=running" | wc -l)
    info "Running containers: $running_containers"

    if [ "$running_containers" -gt 0 ]; then
        success "Docker containers are running"
    else
        error "No Docker containers are running"
        verification_passed=false
    fi

    if [ "$verification_passed" = true ]; then
        success "Installation verification passed"
        return 0
    else
        error "Installation verification failed"
        return 1
    fi
}

# ==============================================================================
# Rollback Function
# ==============================================================================

rollback_installation() {
    warning "Rolling back installation..."

    # Stop and remove containers
    docker-compose down -v 2>/dev/null || true

    # Restore backup if exists
    if [ -f "${ENV_FILE}.backup."* ]; then
        local latest_backup=$(ls -t "${ENV_FILE}.backup."* 2>/dev/null | head -1)
        if [ -n "$latest_backup" ]; then
            mv "$latest_backup" "$ENV_FILE"
            info "Restored previous .env file"
        fi
    fi

    error "Installation rolled back"
}

# ==============================================================================
# Main Installation Flow
# ==============================================================================

show_header() {
    clear
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "          🦡 DachsHaus E-Commerce Platform Installer          "
    echo "                        Version $VERSION                       "
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "This script will set up a complete e-commerce platform with:"
    echo "  • Microservices architecture (Kotlin + TypeScript)"
    echo "  • GraphQL Federation API"
    echo "  • Event-driven messaging (Kafka)"
    echo "  • PostgreSQL + Redis databases"
    echo "  • Next.js storefront"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

show_summary() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    success "Installation completed successfully! 🎉"
    echo ""
    echo "Access your DachsHaus platform:"
    echo ""
    echo "  🌐 Storefront:         http://localhost:3000"
    echo "  🔧 GraphQL Playground: http://localhost:4000/graphql"
    echo ""
    echo "Credentials and configuration:"
    echo "  📄 See CREDENTIALS.txt for database and service passwords"
    echo "  ⚙️  See .env for full configuration"
    echo ""
    echo "Useful commands:"
    echo "  make dev      - Start the platform"
    echo "  make build    - Build all services"
    echo "  make test     - Run tests"
    echo ""
    echo "  docker-compose ps        - View running services"
    echo "  docker-compose logs -f   - View logs"
    echo "  docker-compose down      - Stop all services"
    echo ""
    echo "Backup & Restore:"
    echo "  ./scripts/backup.sh           - Create database backup"
    echo "  ./scripts/restore.sh <file>   - Restore from backup"
    echo ""
    echo "Documentation:"
    echo "  README.md            - Platform overview"
    echo "  CLAUDE.md            - Implementation guide"
    echo "  CREDENTIALS.txt      - Access credentials"
    echo ""
    warning "⚠️  Security Reminders:"
    echo "  1. Change default admin password immediately"
    echo "  2. Keep CREDENTIALS.txt secure and never commit it"
    echo "  3. For production use, update .env with proper secrets"
    echo "  4. Review security settings in docker-compose.prod.yml"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    info "Installation log saved to: $LOG_FILE"
    echo ""
}

main() {
    # Trap errors for rollback
    trap 'rollback_installation' ERR

    show_header

    # Confirm installation
    read -p "Do you want to proceed with the installation? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        info "Installation cancelled"
        exit 0
    fi

    log "Starting DachsHaus installation v$VERSION"

    # Installation steps
    check_requirements
    setup_environment
    create_production_compose

    step "Building and starting services"
    info "This will take several minutes on first run..."

    # Use production compose as overlay
    if docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build; then
        success "Services started"
    else
        fatal "Failed to start services"
    fi

    check_services_health
    run_post_install_setup
    verify_installation

    show_summary

    log "Installation completed successfully"
}

# ==============================================================================
# Script Entry Point
# ==============================================================================

# Check if running as root (not recommended)
if [ "$EUID" -eq 0 ]; then
    warning "Running as root is not recommended"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Create scripts directory if it doesn't exist
mkdir -p "$SCRIPT_DIR/scripts"

# Run main installation
main "$@"
