package com.pbl6.fitme.model

import java.io.Serializable

data class Review(
    val reviewId: String,
    val productId: String,
    val userId: String,
    val rating: Int,
    val comment: String
): Serializable