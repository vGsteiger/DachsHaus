package com.dachshaus.common.kafka

/**
 * Centralized Kafka topic names for the DachsHaus platform.
 *
 * All topics follow the pattern: dachshaus.{domain}.{event}
 */
object TopicNames {
    // Auth domain
    const val USER_REGISTERED = "dachshaus.auth.user-registered"
    const val REVOCATIONS = "dachshaus.auth.revocations"

    // Catalog domain
    const val CATALOG_PRODUCTS = "dachshaus.catalog.products"
    const val CATALOG_INVENTORY = "dachshaus.catalog.inventory"

    // Order domain
    const val ORDER_EVENTS = "dachshaus.order.events"
    const val ORDER_ENRICHED = "dachshaus.order.enriched"

    // Customer domain
    const val CUSTOMER_EVENTS = "dachshaus.customer.events"

    // Cart domain
    const val CART_UPDATED = "dachshaus.cart.updated"
    const val CART_CHECKED_OUT = "dachshaus.cart.checked-out"

    // Notifications
    const val NOTIFICATIONS_OUTBOX = "dachshaus.notifications.outbox"

    // Dead Letter Queue
    const val DLQ = "dachshaus.dlq"
}
