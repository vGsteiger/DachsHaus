# PKG-05: Catalog Service

**Status:** Not Started
**Depends on:** PKG-01, PKG-02

## Goal

Product catalog — CRUD for products, variants, collections. Public read access, admin write access. Publishes product and inventory events to Kafka.

## Produces

- GraphQL subgraph: products (paginated, filterable), collections, variants
- Federation entity: `Product @key(fields: "id")` — resolved by Order and Cart services
- `ProductResolver`, `CollectionResolver`, `ProductFederationResolver`
- `VariantDataLoader` — batch loading to solve N+1 on product→variants
- `CatalogService` — domain logic for product management
- `CatalogEventProducer` — publishes to `catalog.products` and `catalog.inventory`
- Flyway: `products`, `product_variants`, `collections`, `collection_products` tables

## Interface Contracts

### GraphQL Subgraph (federation v2)

**Queries (public):**
```graphql
product(id: ID!): Product
products(filter: ProductFilter, page: PageInput): ProductConnection!
collection(slug: String!): Collection
collections: [Collection!]!
```

**Mutations (admin only — `@admin` directive):**
```graphql
createProduct(input: CreateProductInput!): Product!
updateProduct(id: ID!, input: UpdateProductInput!): Product!
addVariant(productId: ID!, input: CreateVariantInput!): ProductVariant!
updateInventory(variantId: ID!, delta: Int!): ProductVariant!
```

**Federation entity resolver:**
`Product @key(fields: "id")` — resolves product references from Order/Cart subgraphs

### Kafka (produced)
- `dachshaus.catalog.products` → `{ productId, action, product }` (key: productId, cleanup: compact)
  - Consumed by: Streams (PKG-09) for order enrichment
- `dachshaus.catalog.inventory` → `{ variantSku, stockQuantity, delta, timestamp }` (key: variantSku, cleanup: compact)
  - Consumed by: Streams (PKG-09) for inventory reservation

### Pagination
Relay cursor-based: `first`/`after`, `last`/`before`

### Filtering
`category`, `collectionSlug`, `minPrice`, `maxPrice`, `inStock`, `search`

## Acceptance Criteria

- [ ] `products` query returns paginated results with correct cursor behavior
- [ ] `product(id)` returns full product with variants and collections
- [ ] Federation entity resolver resolves `Product` references from other subgraphs
- [ ] `VariantDataLoader` batch-loads variants (verified: 1 SQL query for N products)
- [ ] `createProduct` rejects requests from non-admin users (returns GQL error)
- [ ] `updateInventory` publishes inventory event to Kafka with correct delta
- [ ] Flyway migrations create all tables with correct indexes and constraints
- [ ] Search filter uses `ILIKE` or `tsvector` on product name/description

## Files to Create

```
services/catalog/build.gradle.kts
services/catalog/src/main/kotlin/com/dachshaus/catalog/CatalogApplication.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/config/GraphQLConfig.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/config/KafkaProducerConfig.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/config/DatabaseConfig.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/model/Product.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/model/ProductVariant.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/model/Collection.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/model/PriceHistory.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/repository/ProductRepository.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/repository/CollectionRepository.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/domain/service/CatalogService.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/graphql/resolver/ProductResolver.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/graphql/resolver/CollectionResolver.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/graphql/resolver/ProductFederationResolver.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/graphql/dataloader/VariantDataLoader.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/kafka/CatalogEventProducer.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/infrastructure/persistence/entity/ProductEntity.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/infrastructure/persistence/entity/ProductVariantEntity.kt
services/catalog/src/main/kotlin/com/dachshaus/catalog/infrastructure/persistence/mapper/ProductMapper.kt
services/catalog/src/main/resources/application.yml
services/catalog/src/main/resources/application-local.yml
services/catalog/src/main/resources/application-docker.yml
services/catalog/src/main/resources/application-kubernetes.yml
services/catalog/src/main/resources/db/migration/V1__create_products.sql
services/catalog/src/main/resources/db/migration/V2__create_variants.sql
services/catalog/src/main/resources/db/migration/V3__create_collections.sql
services/catalog/src/main/resources/graphql/schema.graphqls
services/catalog/src/test/kotlin/com/dachshaus/catalog/domain/service/CatalogServiceTest.kt
services/catalog/src/test/kotlin/com/dachshaus/catalog/graphql/ProductResolverIntegrationTest.kt
services/catalog/src/test/kotlin/com/dachshaus/catalog/testcontainers/PostgresTestBase.kt
```
