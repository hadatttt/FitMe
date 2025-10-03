package com.pbl6.fitme.model

import java.util.UUID

data class Brand(
    val brandId: UUID,
    val brandName: String,
    val createdAt: String?,
    val description: String?,
    val isActive: Boolean?
)
