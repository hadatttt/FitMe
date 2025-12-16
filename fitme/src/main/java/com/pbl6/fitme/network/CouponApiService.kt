package com.pbl6.fitme.network

import com.pbl6.fitme.model.Coupon // Đảm bảo bạn đã có model Coupon
import retrofit2.Call
import retrofit2.http.*

interface CouponApiService {

    // GET /coupons
    @GET("coupons")
    fun getAllCoupons(
        @Header("Authorization") token: String
    ): Call<List<Coupon>>

    // POST /coupons
    @POST("coupons")
    fun createCoupon(
        @Header("Authorization") token: String,
        @Body coupon: Coupon // Hoặc dùng CouponRequest nếu có class riêng
    ): Call<Coupon>

    // GET /coupons/active
    @GET("coupons/active")
    fun getActiveCoupons(
        @Header("Authorization") token: String
    ): Call<List<Coupon>>

    // GET /coupons/{couponId}
    @GET("coupons/{couponId}")
    fun getCouponById(
        @Header("Authorization") token: String,
        @Path("couponId") couponId: String
    ): Call<Coupon>

    // PUT /coupons/{couponId}
    @PUT("coupons/{couponId}")
    fun updateCoupon(
        @Header("Authorization") token: String,
        @Path("couponId") couponId: String,
        @Body coupon: Coupon
    ): Call<Coupon>

    // DELETE /coupons/{couponId}
    @DELETE("coupons/{couponId}")
    fun deleteCoupon(
        @Header("Authorization") token: String,
        @Path("couponId") couponId: String
    ): Call<Void>

    // GET /coupons/code/{code}
    @GET("coupons/code/{code}")
    fun getCouponByCode(
        @Header("Authorization") token: String,
        @Path("code") code: String
    ): Call<Coupon>
}