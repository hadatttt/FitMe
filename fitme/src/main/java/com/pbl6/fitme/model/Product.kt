package com.pbl6.fitme.model

import java.util.UUID

// Theo bảng products trong sql.sql

data class Product(
    val productId: UUID,
    val productName: String,
    val description: String?,
    val gender: String?,
    val isActive: Boolean,
    val season: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val brandId: UUID,
    val categoryId: UUID
)
