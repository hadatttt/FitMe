package com.pbl6.fitme.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TryOnApiService {
    @Multipart
    @POST("tryon/virtual")
    fun virtualTryOn(
        @Header("Authorization") auth: String?,
        @Part person: MultipartBody.Part,
        @Part cloth: MultipartBody.Part
    ): Call<ResponseBody>
}
