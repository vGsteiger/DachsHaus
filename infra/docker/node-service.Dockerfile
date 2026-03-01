# Multi-stage Dockerfile for Node.js services
# Stage 1: Build
FROM node:25-alpine AS build

ARG SERVICE_NAME
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm@8.15.0

# Copy package files
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY turbo.json ./

# Copy packages
COPY packages ./packages

# Copy specific service
COPY ${SERVICE_NAME} ./${SERVICE_NAME}

# Install dependencies
RUN pnpm install --frozen-lockfile

# Build the service
RUN pnpm turbo run build --filter=${SERVICE_NAME}

# Stage 2: Runtime
FROM node:25-alpine

ARG SERVICE_NAME
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm@8.15.0

# Copy package files
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./

# Copy built service
COPY --from=build /app/${SERVICE_NAME} ./${SERVICE_NAME}
COPY --from=build /app/packages ./packages
COPY --from=build /app/node_modules ./node_modules

# Install production dependencies only
RUN pnpm install --prod --frozen-lockfile

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Health check (adjust port based on service)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-3000}/health || exit 1

EXPOSE ${PORT:-3000}

WORKDIR /app/${SERVICE_NAME}

CMD ["pnpm", "start"]
