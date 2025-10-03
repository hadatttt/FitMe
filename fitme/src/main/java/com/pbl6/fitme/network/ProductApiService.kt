package com.pbl6.fitme.network

import com.pbl6.fitme.model.Product
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {
    @GET("products")
    fun getProducts(): Call<List<Product>>

    @GET("products/{id}")
    fun getProductById(@Path("id") id: String): Call<Product>
}
