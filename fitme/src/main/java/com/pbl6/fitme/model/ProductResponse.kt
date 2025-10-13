package com.pbl6.fitme.model

import java.util.UUID

// DTO matching backend JSON for product list endpoints
data class ProductResponse(
    val productId: UUID,
    val productName: String,
    val description: String?,
    val categoryName: String,
    val brandName: String,
    val isActive: Boolean,
    val images: List<String> = emptyList(),
    val variants: List<VariantResponse> = emptyList()
)
