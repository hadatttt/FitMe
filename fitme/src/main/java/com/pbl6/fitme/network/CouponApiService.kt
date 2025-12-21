package com.pbl6.fitme.network

import com.pbl6.fitme.model.Coupon
import retrofit2.Call
import retrofit2.http.*

interface CouponApiService {

    @GET("coupons/active")
    fun getActiveCoupons(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<Coupon>>>

    @GET("coupons")
    fun getAllCoupons(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<Coupon>>>

    @GET("coupons/code/{code}")
    fun getCouponByCode(
        @Header("Authorization") token: String,
        @Path("code") code: String
    ): Call<BaseResponse<Coupon>>
}