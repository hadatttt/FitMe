package com.pbl6.fitme.model

import java.util.UUID

data class OrderItem(
    val orderItemId: UUID,
    val quantity: Int,
    val totalPrice: Double,
    val unitPrice: Double,
    val orderId: UUID,
    val variantId: UUID
)

data class Order(
    val orderId: UUID,
    val createdAt: String?,
    val orderStatus: String,
    val totalAmount: Double,
    val updatedAt: String?,
    val userId: UUID,
    val items: List<OrderItem>
)
