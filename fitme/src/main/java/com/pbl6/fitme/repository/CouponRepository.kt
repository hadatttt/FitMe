package com.pbl6.fitme.repository

import com.pbl6.fitme.model.Coupon
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.CouponApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CouponRepository {
    private val couponApiService = ApiClient.retrofit.create(CouponApiService::class.java)

    fun getAllCoupons(token: String, onResult: (List<Coupon>?) -> Unit) {
        val bearerToken = "Bearer $token"

        couponApiService.getAllCoupons(bearerToken).enqueue(object : Callback<List<Coupon>> {
            override fun onResponse(call: Call<List<Coupon>>, response: Response<List<Coupon>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Coupon>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun getCouponByCode(token: String, code: String, onResult: (Coupon?) -> Unit) {
        val bearerToken = "Bearer $token"

        couponApiService.getCouponByCode(bearerToken, code).enqueue(object : Callback<Coupon> {
            override fun onResponse(call: Call<Coupon>, response: Response<Coupon>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Coupon>, t: Throwable) {
                onResult(null)
            }
        })
    }
}