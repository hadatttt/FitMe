package com.pbl6.fitme.network

import com.pbl6.fitme.model.Category
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CategoryApiService {
    @GET("categories")
    fun getCategories(): Call<List<Category>>

    @GET("categories/{id}")
    fun getCategoryById(@Path("id") id: String): Call<Category>
}
