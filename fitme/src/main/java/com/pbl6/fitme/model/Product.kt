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
        get() {
            // Be defensive: Gson may set fields to null even if Kotlin declares non-nullable defaults.
            val imgs = images as? List<ProductImage> ?: emptyList()
            return imgs.firstOrNull { it.isMain == true }?.imageUrl ?: imgs.firstOrNull()?.imageUrl
        }
    val minPrice: Double?
        get() {
            val vars = variants as? List<ProductVariant> ?: emptyList()
            return vars.minByOrNull { it.price }?.price
        }
}