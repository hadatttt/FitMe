package com.pbl6.fitme.model

import java.io.Serializable
import java.util.UUID

data class Order(
    // common
    val orderId: String? = null,
    val userEmail: String = "",

    // response fields (nullable) to support listing/detail screens
    val userId: String? = null,
    val orderDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val status: String? = null,
    val orderStatus: String? = null,

    // shipping & coupon
    val shippingAddressId: String = "",
    val shippingAddress: ShippingAddress = ShippingAddress(),
    val couponId: String = "",
    // Added to mirror backend OrderResponse
    val shippingAddressDetails: String? = null,
    val couponCode: String? = null,

    // order items (both names supported by different parts of app)
    val orderItems: List<OrderItem> = emptyList(),
    val items: List<OrderItem> = emptyList(),

    // monetary fields
    val subtotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val shippingFee: Double = 0.0,
    val orderNotes: String = ""
) : Serializable

data class ShippingAddress(
    val addressId: String = "",
    val userId: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val stateProvince: String = "",
    val postalCode: String = "",
    val country: String = "Vietnam",
    val isDefault: Boolean = false,
    val addressType: String = ""
) : Serializable
