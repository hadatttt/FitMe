package com.pbl6.fitme.network

import com.pbl6.fitme.model.MomoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MomoApiService {
    @GET("momopay/create")
    fun createMomoPayment(
        @Header("Authorization") token: String,
        @Query("amount") amount: Long,
        @Query("userEmail") userEmail: String,
        @Query("orderId") orderId: String? = null
    ): Call<MomoResponse>
}
