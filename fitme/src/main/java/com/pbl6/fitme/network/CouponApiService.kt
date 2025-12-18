package com.pbl6.fitme.network

import com.pbl6.fitme.model.Coupon
import retrofit2.Call
import retrofit2.http.*

interface CouponApiService {

    // QUAN TRỌNG: User thường nên dùng API này
    @GET("coupons/active")
    fun getActiveCoupons(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<Coupon>>>

    // API này của Admin, nếu user gọi sẽ bị lỗi 403, nhưng cứ sửa cấu trúc cho đúng
    @GET("coupons")
    fun getAllCoupons(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<Coupon>>>

    // Lấy chi tiết coupon theo code
    @GET("coupons/code/{code}")
    fun getCouponByCode(
        @Header("Authorization") token: String,
        @Path("code") code: String
    ): Call<BaseResponse<Coupon>>
}