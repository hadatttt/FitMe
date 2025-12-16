package com.pbl6.fitme.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Coupon(
val couponId: String? = null,
val code: String? = null,
val discountType: String? = null, // "PERCENTAGE" or "AMOUNT"
val discountValue: Double = 0.0,
val minimumOrderAmount: Double = 0.0,
val maximumDiscountAmount: Double = 0.0,
 val startDate: String? = null,
 val endDate: String? = null,
 val usageLimit: Int = 0,
val usedCount: Int = 0,
val isActive: Boolean = false,
 val createdAt: String? = null
)