package com.pbl6.fitme.model

import java.util.UUID

// Model Product đã được cập nhật để khớp với JSON từ API
data class Product(
    val productId: UUID,
    val productName: String,
    val description: String?,
    val categoryName: String, // <-- Sửa từ categoryId
    val brandName: String,    // <-- Sửa từ brandId
    val isActive: Boolean,
    val images: List<String>,       // <-- Thêm trường images
    val variants: List<ProductVariant> // <-- Thêm trường variants
)