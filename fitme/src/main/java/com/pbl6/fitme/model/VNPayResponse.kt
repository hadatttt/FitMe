package com.pbl6.fitme.model

data class VNPayResponse(
    val paymentUrl: String? = null,
    val responseCode: String? = null,
    val message: String? = null
)

data class BaseResponse<T>(
    val status: Int,
    val message: String,
    val result: T?
)