package com.pbl6.fitme.model

import java.util.UUID

// Product model with images and reviews as typed objects
data class Product(
    val productId: UUID,
    val productName: String,
    val createdAt: String? = null,
    val description: String?,
    val categoryName: String, // mapped from categoryId by API layer
    val brandName: String,    // mapped from brandId by API layer
    val gender: String? = null,
    val season: String? = null,
    val isActive: Boolean,
    val images: List<ProductImage> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val reviews: List<ProductReview> = emptyList()
) {
    // convenience property returning the main image url or first image
    val mainImageUrl: String?
        get() = images.firstOrNull { it.isMain == true }?.imageUrl ?: images.firstOrNull()?.imageUrl
}