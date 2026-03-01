# DachsHaus Makefile - Top-level orchestrator

.PHONY: help dev build test docker clean install

help:
	@echo "DachsHaus - Available commands:"
	@echo "  make dev      - Start local development environment"
	@echo "  make build    - Build all services"
	@echo "  make test     - Run all tests"
	@echo "  make docker   - Build Docker images"
	@echo "  make clean    - Clean build artifacts"
	@echo "  make install  - Install all dependencies"

dev:
	docker-compose up

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
	docker build -f infra/docker/kotlin-service.Dockerfile -t dachshaus/kotlin-service .
	docker build -f infra/docker/node-service.Dockerfile -t dachshaus/node-service .

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
