# DachsHaus Makefile - Top-level orchestrator

.PHONY: help dev build test docker clean install setup backup restore

help:
	@echo "DachsHaus - Available commands:"
	@echo ""
	@echo "  🚀 Quick Start:"
	@echo "    make setup    - Run one-click installation"
	@echo "    make dev      - Start local development environment"
	@echo ""
	@echo "  🛠️  Development:"
	@echo "    make build    - Build all services"
	@echo "    make test     - Run all tests"
	@echo "    make clean    - Clean build artifacts"
	@echo "    make install  - Install all dependencies"
	@echo ""
	@echo "  🐳 Docker:"
	@echo "    make docker   - Build Docker images"
	@echo "    make up       - Start all services (detached)"
	@echo "    make down     - Stop all services"
	@echo "    make restart  - Restart all services"
	@echo "    make logs     - View logs from all services"
	@echo "    make ps       - Show running services"
	@echo ""
	@echo "  💾 Backup & Restore:"
	@echo "    make backup   - Create database backup"
	@echo "    make restore  - Restore database (requires BACKUP_FILE=...)"
	@echo ""
	@echo "  🏥 Health & Monitoring:"
	@echo "    make status   - Check health of all services"
	@echo "    make check    - Run system diagnostics"
	@echo ""
	@echo "For more information, see INSTALL.md"

setup:
	@echo "🚀 Running one-click installation..."
	@./install.sh

dev:
	docker-compose up

up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

ps:
	docker-compose ps

build:
	@echo "Building all services..."
	cd services && ./gradlew build
	pnpm turbo run build

test:
	@echo "Running all tests..."
	cd services && ./gradlew test
	pnpm turbo run test

docker:
	@echo "Building Docker images..."
	docker-compose build

install:
	@echo "Installing dependencies..."
	pnpm install

clean:
	@echo "Cleaning build artifacts..."
	cd services && ./gradlew clean
	pnpm turbo run clean
	rm -rf node_modules
	rm -rf */node_modules
	rm -rf **/node_modules

backup:
	@if [ -f scripts/backup.sh ]; then \
		./scripts/backup.sh; \
	else \
		echo "⚠️  Backup script not found. Run 'make setup' first."; \
	fi

restore:
	@if [ -z "$(BACKUP_FILE)" ]; then \
		echo "❌ Please specify BACKUP_FILE=path/to/backup.sql.gz"; \
		exit 1; \
	fi
	@if [ -f scripts/restore.sh ]; then \
		./scripts/restore.sh $(BACKUP_FILE); \
	else \
		echo "⚠️  Restore script not found. Run 'make setup' first."; \
	fi

status:
	@echo "📊 Service Health Status:"
	@echo ""
	@docker-compose ps
	@echo ""
	@echo "Checking HTTP endpoints..."
	@curl -sf http://localhost:3000 > /dev/null && echo "✓ Storefront (port 3000): UP" || echo "✗ Storefront (port 3000): DOWN"
	@curl -sf http://localhost:4000/healthz > /dev/null && echo "✓ Gateway (port 4000): UP" || echo "✗ Gateway (port 4000): DOWN"

check:
	@echo "🔍 Running system diagnostics..."
	@echo ""
	@echo "Docker version:"
	@docker --version
	@echo ""
	@echo "Docker Compose version:"
	@docker-compose --version || docker compose version
	@echo ""
	@echo "Disk space:"
	@df -h . | tail -1
	@echo ""
	@echo "Memory:"
	@free -h | grep Mem
	@echo ""
	@echo "Running containers:"
	@docker ps --filter "name=dachshaus" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

