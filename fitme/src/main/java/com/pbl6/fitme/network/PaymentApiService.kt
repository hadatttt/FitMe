package com.pbl6.fitme.network

import com.pbl6.fitme.network.BaseResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PaymentApiService {
    @POST("payment")
    fun createPayment(
        @Header("Authorization") token: String,
        @Body req: Map<String, Any>
    ): Call<BaseResponse<String>>
}
