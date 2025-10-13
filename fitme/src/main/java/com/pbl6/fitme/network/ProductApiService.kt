package com.pbl6.fitme.network

import com.pbl6.fitme.model.ProductResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ProductApiService {
    @GET("products")
    fun getProducts(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<ProductResponse>>>

    @GET("products/{id}")
    fun getProductById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Call<BaseResponse<ProductResponse>>
}