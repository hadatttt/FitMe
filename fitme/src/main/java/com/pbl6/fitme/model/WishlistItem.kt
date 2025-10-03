package com.pbl6.fitme.model

import java.util.UUID

data class WishlistItem(
    val wishlistItemId: UUID,
    val addedAt: String?,
    val productId: UUID,
    val wishlistId: UUID
)
