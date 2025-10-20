package com.pbl6.fitme.model

import java.util.UUID

data class AddCartRequest(
    val variant: UUID,
    val quantity: Int
)