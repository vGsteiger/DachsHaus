# 🚀 DachsHaus Quick Start Guide

Get your online shop running in 5 minutes!

## Prerequisites Check

Before you begin, make sure you have:

```bash
# Check Docker
docker --version
# Need: Docker 20.10+

# Check Docker Compose
docker-compose --version
# Need: Docker Compose 2.0+

# Check Make (optional but recommended)
make --version

# Check available resources
df -h .  # Need: 10GB+ free space
free -h  # Need: 4GB+ RAM
```

## Installation (3 Easy Steps)

### Step 1: Clone the Repository

```bash
git clone https://github.com/vGsteiger/DachsHaus.git
cd DachsHaus
```

### Step 2: Run the Installer

```bash
./install.sh
```

The installer will:
- ✓ Verify your system meets requirements
- ✓ Generate secure credentials automatically
- ✓ Set up all 10 services (databases, microservices, frontend)
- ✓ Configure networking and security
- ✓ Start everything with health checks

**Time**: ~5-10 minutes (depending on your internet speed)

### Step 3: Access Your Shop

Once installation completes:

- 🛒 **Your Online Shop**: http://localhost:3000
- 🔧 **API Playground**: http://localhost:4000/graphql
- 📄 **Your Credentials**: See `CREDENTIALS.txt`

**Default Admin Account**:
- Email: `admin@dachshaus.local`
- Password: `admin123`
- ⚠️ Change this immediately!

## What You Get

After installation, you have a complete e-commerce platform with:

### 🛍️ Customer Features
- Product browsing and search
- Shopping cart
- User registration and login
- Order placement and tracking
- User profile management

### 👨‍💼 Admin Features
- Product catalog management
- Inventory management
- Order management
- Customer management
- Analytics dashboard

### 🔧 Technical Features
- **7 microservices** (Auth, Catalog, Order, Customer, Cart, Streams)
- **GraphQL Federation** API
- **Event-driven** architecture (Kafka)
- **Real-time** order updates (WebSockets)
- **Production-grade** security (HMAC, JWT, RBAC)

## First Steps After Installation

### 1. Login as Admin

```bash
# Open your browser
open http://localhost:3000/auth/login

# Login with:
# Email: admin@dachshaus.local
# Password: admin123
```

### 2. Change Admin Password

Navigate to profile settings and change your password immediately!

### 3. Create Your First Product

**Via GraphQL Playground** (http://localhost:4000/graphql):

```graphql
# First, login to get your token
mutation Login {
  login(email: "admin@dachshaus.local", password: "admin123") {
    accessToken
    refreshToken
  }
}

# Copy the accessToken and add it to HTTP Headers:
# {
#   "Authorization": "Bearer YOUR_ACCESS_TOKEN_HERE"
# }

# Then create a product
mutation CreateProduct {
  createProduct(input: {
    name: "Cool Product"
    description: "An amazing product"
    priceCents: 2999  # $29.99
    sku: "PROD-001"
    stock: 100
  }) {
    id
    name
    priceCents
    sku
  }
}
```

### 4. Browse Your Shop

Go to http://localhost:3000 to see your product in the storefront!

## Common Tasks

### View Logs

```bash
# All services
make logs

# Specific service
docker-compose logs -f gateway
```

### Check Service Status

```bash
make status
```

### Stop the Platform

```bash
make down
```

### Start Again

```bash
make up
```

### Create a Backup

```bash
make backup
```

### Restore from Backup

```bash
make restore BACKUP_FILE=./backups/postgres/backup_20260309_120000.sql.gz
```

## Exploring the API

### GraphQL Playground

Navigate to http://localhost:4000/graphql

**Example Queries:**

```graphql
# Get all products
query GetProducts {
  products(first: 10) {
    edges {
      node {
        id
        name
        description
        priceCents
        variants {
          sku
          stock
        }
      }
    }
  }
}

# Add to cart (requires authentication)
mutation AddToCart {
  addToCart(input: {
    variantSku: "PROD-001"
    quantity: 1
  }) {
    cart {
      items {
        productId
        quantity
        priceCents
      }
      totalCents
    }
  }
}

# Place an order (requires authentication)
mutation Checkout {
  checkout {
    order {
      id
      status
      totalCents
      items {
        productName
        quantity
      }
    }
  }
}
```

### Authentication Flow

```graphql
# 1. Register a new user
mutation Register {
  register(input: {
    email: "user@example.com"
    password: "SecurePassword123!"
    firstName: "John"
    lastName: "Doe"
  }) {
    accessToken
    refreshToken
    user {
      id
      email
    }
  }
}

# 2. Login (for existing users)
mutation Login {
  login(email: "user@example.com", password: "SecurePassword123!") {
    accessToken
    refreshToken
  }
}

# 3. Use the accessToken in HTTP Headers:
# {
#   "Authorization": "Bearer YOUR_ACCESS_TOKEN"
# }

# 4. Refresh token when access token expires
mutation RefreshToken {
  refreshToken(refreshToken: "YOUR_REFRESH_TOKEN") {
    accessToken
    refreshToken
  }
}
```

## Architecture Overview

```
┌──────────────────────────────────────────────┐
│           Your Browser (Client)              │
└─────────────────┬────────────────────────────┘
                  │
         ┌────────▼────────┐
         │   Storefront    │  (Next.js - Port 3000)
         │   - UI/UX       │
         │   - SSR         │
         └────────┬────────┘
                  │
         ┌────────▼────────┐
         │    Gateway      │  (NestJS - Port 4000)
         │  - GraphQL API  │
         │  - Auth Check   │
         │  - Federation   │
         └────────┬────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
    ▼             ▼             ▼
┌────────┐   ┌────────┐   ┌────────┐
│  Auth  │   │Catalog │   │  Cart  │  (+ 4 more services)
│8081    │   │8082    │   │8085    │
└────────┘   └────────┘   └────────┘
    │             │             │
    └─────────────┼─────────────┘
                  │
         ┌────────▼────────┐
         │  Infrastructure │
         │  - PostgreSQL   │
         │  - Redis        │
         │  - Kafka        │
         └─────────────────┘
```

## Troubleshooting

### Port Already in Use

```bash
# Check what's using the port
sudo lsof -i :3000  # or :4000, :5432, etc.

# Kill the process or change ports in docker-compose.yml
```

### Services Won't Start

```bash
# Check Docker is running
docker info

# Check logs for errors
docker-compose logs gateway
docker-compose logs auth

# Restart specific service
docker-compose restart gateway
```

### Can't Access Storefront

```bash
# Check if container is running
docker-compose ps storefront

# Check logs
docker-compose logs storefront

# Try accessing directly
curl http://localhost:3000
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker-compose exec postgres psql -U dachshaus -c "SELECT 1;"

# Restart database
docker-compose restart postgres
```

### Reset Everything

If things go wrong:

```bash
# Stop and remove everything
docker-compose down -v

# Remove generated files
rm -rf keys/ .env CREDENTIALS.txt backups/

# Start fresh
./install.sh
```

## Next Steps

### For Developers

1. **Explore the code**: Check out `services/`, `gateway/`, `storefront/`
2. **Read architecture docs**: See `CLAUDE.md` for detailed specs
3. **Run tests**: `make test`
4. **Build locally**: `make build`

### For Shop Owners

1. **Customize branding**: Update `storefront/` styling
2. **Add products**: Use the admin panel or GraphQL API
3. **Configure payments**: Integrate Stripe/PayPal
4. **Set up email**: Configure SMTP for order notifications
5. **Go to production**: See `INSTALL.md` for deployment guides

### For Security-Conscious Users

1. **Change all default passwords**: Admin, databases
2. **Review security checklist**: See `SECURITY.md`
3. **Set up SSL/TLS**: For production deployments
4. **Enable monitoring**: Set up alerts and logging
5. **Regular backups**: Schedule with `cron`

## Getting Help

- **Documentation**: See `README.md`, `INSTALL.md`, `CLAUDE.md`
- **Logs**: `make logs` or `docker-compose logs -f`
- **Issues**: https://github.com/vGsteiger/DachsHaus/issues
- **Health Check**: `make status`

## Useful Commands

```bash
# Show all available commands
make help

# System diagnostics
make check

# View running services
make ps

# Follow logs in real-time
make logs

# Check health status
make status

# Restart all services
make restart

# Create database backup
make backup
```

## What's Running?

After successful installation:

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| Storefront | 3000 | http://localhost:3000 | Customer-facing shop |
| Gateway | 4000 | http://localhost:4000/graphql | GraphQL API |
| Auth | 8081 | Internal | Authentication |
| Catalog | 8082 | Internal | Products |
| Order | 8083 | Internal | Orders |
| Customer | 8084 | Internal | User profiles |
| Cart | 8085 | Internal | Shopping cart |
| PostgreSQL | 5432 | Internal | Database |
| Redis | 6379 | Internal | Cache |
| Kafka | 9092 | Internal | Events |

## Files Created by Installer

```
DachsHaus/
├── .env                    # Environment configuration
├── CREDENTIALS.txt         # All passwords (KEEP SECURE!)
├── install.log            # Installation log
├── keys/
│   ├── auth-private.pem   # JWT private key
│   └── auth-public.pem    # JWT public key
├── scripts/
│   ├── backup.sh          # Database backup script
│   └── restore.sh         # Database restore script
├── backups/
│   └── postgres/          # Backup storage
└── docker-compose.prod.yml # Production config
```

⚠️ **Never commit these files to git!**

## Success Indicators

You'll know everything is working when:

- ✓ All services show "Up" in `make ps`
- ✓ Storefront loads at http://localhost:3000
- ✓ You can login with the admin account
- ✓ GraphQL playground works at http://localhost:4000/graphql
- ✓ No error messages in `make logs`

---

**Congratulations! You now have a fully functional online shop! 🎉**

For more detailed information, see:
- **Installation Guide**: `INSTALL.md`
- **Security Checklist**: `SECURITY.md`
- **Architecture Details**: `CLAUDE.md`
- **Main Documentation**: `README.md`
