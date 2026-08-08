package com.flashshoes.model;

/**
 * Lifecycle of an Order. PENDING -> CONFIRMED happens fast (payment settlement,
 * see OrderProcessor). CONFIRMED -> PACKED -> SHIPPED -> DELIVERED is the
 * fulfilment/tracking pipeline shown on the customer's Order Tracking screen.
 * FAILED is terminal and only reachable from PENDING (payment/stock failure).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED
}
