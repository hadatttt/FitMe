package com.pbl6.fitme.network

import com.pbl6.fitme.model.ProductVariant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductVariantApiService {
    @GET("product-variants")
    fun getProductVariants(@retrofit2.http.Header("Authorization") token: String): Call<BaseResponse<List<ProductVariant>>>

    @GET("product-variants/{id}")
    fun getProductVariantById(
        @retrofit2.http.Header("Authorization") token: String,
        @Path("id") id: String
    ): Call<BaseResponse<ProductVariant>>
}
