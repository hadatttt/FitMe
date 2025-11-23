package com.pbl6.fitme.network

data class UserAddressRequest(
    val userEmail: String,
    val recipientName: String,
    val phone: String,
    val addressLine1: String,
    val addressLine2: String? = null, // Có thể null
    val city: String,
    val stateProvince: String,
    val postalCode: String,
    val country: String,
    val isDefault: Boolean,
    val addressType: String
)
data class UserAddressResponse(
    val addressId: String,
    val userId: String,
    val recipientName: String,
    val phone: String,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val stateProvince: String,
    val postalCode: String,
    val country: String,
    val isDefault: Boolean,
    val addressType: String
)