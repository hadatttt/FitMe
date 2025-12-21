package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.Coupon
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.BaseResponse
import com.pbl6.fitme.network.CouponApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CouponRepository {
    private val couponApiService = ApiClient.retrofit.create(CouponApiService::class.java)

    fun getAllCoupons(token: String, onResult: (List<Coupon>?) -> Unit) {
        val bearerToken = "Bearer $token"

        couponApiService.getActiveCoupons(bearerToken).enqueue(object : Callback<BaseResponse<List<Coupon>>> {
            override fun onResponse(
                call: Call<BaseResponse<List<Coupon>>>,
                response: Response<BaseResponse<List<Coupon>>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200 || apiResponse?.result != null) {
                        onResult(apiResponse.result)
                    } else {
                        Log.e("CouponRepo", "Backend Msg: ${apiResponse?.message}")
                        onResult(null)
                    }
                } else {
                    Log.e("CouponRepo", "HTTP Error: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<List<Coupon>>>, t: Throwable) {
                Log.e("CouponRepo", "Network Error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun getCouponByCode(token: String, code: String, onResult: (Coupon?) -> Unit) {
        val bearerToken = "Bearer $token"

        couponApiService.getCouponByCode(bearerToken, code).enqueue(object : Callback<BaseResponse<Coupon>> {
            override fun onResponse(
                call: Call<BaseResponse<Coupon>>,
                response: Response<BaseResponse<Coupon>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 200) {
                        onResult(apiResponse.result)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<Coupon>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}