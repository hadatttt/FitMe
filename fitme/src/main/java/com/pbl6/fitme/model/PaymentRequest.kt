package com.pbl6.fitme.model

data class PaymentRequest(
    val orderId: String,
    val paymentMethod: String,
    val amount: Double,
    val paymentStatus: String? = null
)
