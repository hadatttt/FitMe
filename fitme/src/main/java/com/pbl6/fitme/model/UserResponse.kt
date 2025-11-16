package com.pbl6.fitme.model

import java.time.LocalDateTime

data class UserResponse(
    val userId: String?,
    val username: String?,
    val email: String?,
    val fullName: String?,
    val phone: String?,
    val address: String?,
    val avatarUrl: String?,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
