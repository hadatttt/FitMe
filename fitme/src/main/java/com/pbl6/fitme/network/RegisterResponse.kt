package com.pbl6.fitme.network

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val fullName: String,
    val dateOfBirth: String?,
    val phone: String?,
    val avatarUrl: String?,
    val gender: String?
)
data class RegisterResponse(
    val message: String,
    val userId: String
)