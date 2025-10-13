package com.pbl6.fitme.network

data class BaseResponse<T>(
    val code: Int,
    val message: String,
    val result: T
)
