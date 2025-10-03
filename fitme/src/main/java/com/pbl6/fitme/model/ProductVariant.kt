package com.pbl6.fitme.model

import java.util.UUID

data class ProductVariant(
    val variantId: UUID,
    val color: String,
    val createdAt: String?,
    val isActive: Boolean?,
    val price: Double,
    val size: String,
    val stockQuantity: Int,
    val updatedAt: String?,
    val productId: UUID
)
