package com.pbl6.fitme.model

import java.util.UUID

data class ProductImage(
    val imageId: Long,
    val createdAt: String?,
    val imageUrl: String,
    val isMain: Boolean?,
    val updatedAt: String?,
    val productId: UUID
)
