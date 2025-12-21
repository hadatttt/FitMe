package com.pbl6.fitme.model

import java.util.UUID

data class CartItem(
    val cartItemId: UUID,
    val addedAt: String?,
    var quantity: Int,
    val cartId: UUID,
    val variantId: UUID
)
