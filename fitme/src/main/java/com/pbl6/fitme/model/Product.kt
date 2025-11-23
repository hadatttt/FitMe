package com.pbl6.fitme.model

import java.io.Serializable
import java.util.UUID

data class Product(
    val productId: UUID,
    val productName: String,
    val createdAt: String? = null,
    val description: String?,
    val categoryName: String,
    val brandName: String,
    val gender: String? = null,
    val season: String? = null,
    val isActive: Boolean,
    val images: List<ProductImage> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val reviews: List<Review> = emptyList()
): Serializable {
    val mainImageUrl: String?
        get() = images.firstOrNull { it.isMain == true }?.imageUrl ?: images.firstOrNull()?.imageUrl
    val minPrice: Double?
        get() = variants.minByOrNull { it.price }?.price
}