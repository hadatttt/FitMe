package com.pbl6.fitme.network

data class LoginResponse(
    val code: Int,
    val message: String,
    val result: TokenResult?
)

data class TokenResult(
    val token: String,
    val refreshToken: String,
    val expiryTime: String,
    val refreshTokenExpiryTime: String,
    val email: String? = null
)
