package com.pbl6.fitme.model

import java.io.Serializable
import java.util.UUID

data class ProductReview(
    val reviewId: UUID,
    val comment: String?,
    val createdAt: String?,
    val rating: Int,
    val updatedAt: String?,
    val productId: UUID,
    val profileId: UUID
): Serializable
