package com.pbl6.fitme.model

import java.io.Serializable
import java.util.UUID
data class ProductVariant(
    val variantId: UUID,
    val color: String,
    val size: String,
    val price: Double,
    val stockQuantity: Int,
    val productId: UUID
): Serializable
