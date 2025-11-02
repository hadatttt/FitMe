package com.pbl6.fitme.network
import com.pbl6.fitme.model.Category
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface CategoryApiService {
    @GET("categories")
    fun getCategories(
        @Header("Authorization") token: String
    ): Call<BaseResponse<List<Category>>>

    @GET("categories/{id}")
    fun getCategoryById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Call<BaseResponse<Category>>
}
