package com.pbl6.fitme.model

import java.io.Serializable
import java.util.UUID

// Extended OrderItem to support both request and response shapes.
// For creating an order we send: productId, quantity, unitPrice, color, size
// Backend returns items with variantId, totalPrice, productImageUrl etc.
data class OrderItem(
    // request fields
    val productId: String? = null,
    val quantity: Int = 0,
    val unitPrice: Double? = null,
    val color: String? = null,
    val size: String? = null,

    // response fields (nullable so same model can parse response)
    val variantId: String? = null,
    val productName: String? = null,
    val variantDetails: String? = null,
    val totalPrice: Double? = null,
    val productImageUrl: String? = null,
    val orderItemId: String? = null,
    val orderId: String? = null
) : Serializable {
    // Additional fields used by mock/data generation elsewhere in app

}