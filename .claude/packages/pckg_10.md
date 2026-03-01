# PKG-10: Storefront

**Status:** Not Started
**Depends on:** PKG-01
**Consumes (runtime):** Gateway (PKG-03) at `:4000/graphql`

## Goal

Lightweight Next.js frontend that consumes the federated GraphQL API — proves the whole backend works end-to-end.

## Produces

- Apollo Client setup with SSR support, auth link, WebSocket split, error handling
- Auth context: login, register, logout, token refresh, cookie-based storage
- 10 routes: homepage, products, product detail, collection, cart, checkout, orders, order detail, login, register, admin
- Components: ProductCard/Grid, CartDrawer, CheckoutForm, OrderStatusBadge (live via subscription)
- Typed GraphQL operations: queries, mutations, subscriptions
- Custom hooks: `useProducts`, `useCart`, `useOrderTracking`

## Interface Contracts

### GraphQL
```
Endpoint: NEXT_PUBLIC_GRAPHQL_URL (default: http://localhost:4000/graphql)
WebSocket: derived from GraphQL URL (ws://...)
```

### Auth Flow
1. Login/Register → receives `{ accessToken, refreshToken }`
2. `accessToken` stored in memory (AuthContext state)
3. `refreshToken` stored in `HttpOnly` cookie (`httpOnly: true`, `secure: true`, `sameSite: strict`), only readable server-side
4. On mount: server-side check detects refresh cookie → Next.js API route calls `refreshToken` mutation and returns fresh `accessToken` to the client
5. On 401 from errorLink → dispatch `auth:expired` event → logout

### Pages
| Route | Access | Features |
|---|---|---|
| `/` | public | Featured collections, SSR |
| `/products` | public | Product grid, cursor pagination, client-side filters |
| `/products/[slug]` | public | Product detail, variant selector, add to cart, SSR |
| `/collections/[slug]` | public | Collection products |
| `/cart` | public | Full cart, qty editing |
| `/checkout` | protected | Address selection, place order |
| `/orders` | protected | Order history |
| `/orders/[id]` | protected | Order detail + live status subscription |
| `/auth/login` | public | Login form |
| `/auth/register` | public | Registration form |
| `/admin/products` | admin | Product CRUD table |

## Acceptance Criteria

- [ ] Product listing loads with SSR (view source shows product data)
- [ ] Adding to cart updates cart icon count immediately (optimistic update)
- [ ] Login stores tokens, subsequent requests include Authorization header
- [ ] Page refresh with valid refresh cookie restores session
- [ ] Order detail page shows live status updates via WebSocket subscription
- [ ] Protected routes redirect to `/auth/login` when unauthenticated
- [ ] All GraphQL operations use generated types from `@dachshaus/graphql-schema`

## Files to Create

```
storefront/package.json
storefront/tsconfig.json
storefront/next.config.js
storefront/tailwind.config.ts
storefront/postcss.config.js
storefront/.env.local
storefront/src/lib/apollo/client.ts
storefront/src/lib/apollo/provider.tsx
storefront/src/lib/apollo/links.ts
storefront/src/lib/auth/context.tsx
storefront/src/lib/auth/hooks.ts
storefront/src/lib/auth/storage.ts
storefront/src/graphql/queries/products.ts
storefront/src/graphql/queries/collections.ts
storefront/src/graphql/queries/cart.ts
storefront/src/graphql/queries/orders.ts
storefront/src/graphql/mutations/auth.ts
storefront/src/graphql/mutations/cart.ts
storefront/src/graphql/mutations/checkout.ts
storefront/src/graphql/subscriptions/orderStatus.ts
storefront/src/hooks/useProducts.ts
storefront/src/hooks/useCart.ts
storefront/src/hooks/useOrderTracking.ts
storefront/src/components/layout/Header.tsx
storefront/src/components/layout/Footer.tsx
storefront/src/components/layout/Layout.tsx
storefront/src/components/product/ProductCard.tsx
storefront/src/components/product/ProductGrid.tsx
storefront/src/components/product/VariantSelector.tsx
storefront/src/components/product/PriceDisplay.tsx
storefront/src/components/cart/CartDrawer.tsx
storefront/src/components/cart/CartItem.tsx
storefront/src/components/cart/CartSummary.tsx
storefront/src/components/checkout/CheckoutForm.tsx
storefront/src/components/checkout/AddressStep.tsx
storefront/src/components/checkout/OrderConfirmation.tsx
storefront/src/components/auth/LoginForm.tsx
storefront/src/components/auth/RegisterForm.tsx
storefront/src/components/auth/AuthModal.tsx
storefront/src/components/order/OrderCard.tsx
storefront/src/components/order/OrderStatusBadge.tsx
storefront/src/app/layout.tsx
storefront/src/app/page.tsx
storefront/src/app/globals.css
storefront/src/app/products/page.tsx
storefront/src/app/products/[slug]/page.tsx
storefront/src/app/collections/[slug]/page.tsx
storefront/src/app/cart/page.tsx
storefront/src/app/checkout/page.tsx
storefront/src/app/orders/page.tsx
storefront/src/app/orders/[id]/page.tsx
storefront/src/app/auth/login/page.tsx
storefront/src/app/auth/register/page.tsx
storefront/src/app/admin/layout.tsx
storefront/src/app/admin/products/page.tsx
storefront/src/app/admin/products/new/page.tsx
test/components/ProductCard.test.tsx
packages/graphql-schema/package.json
packages/graphql-schema/codegen.yml
packages/graphql-schema/src/generated/types.ts
packages/graphql-schema/src/generated/catalog.ts
packages/graphql-schema/src/generated/order.ts
packages/graphql-schema/src/generated/customer.ts
packages/graphql-schema/src/generated/cart.ts
packages/graphql-schema/src/generated/auth.ts
packages/graphql-schema/src/generated/hooks.ts
```
