package com.pbl6.fitme.network

import com.pbl6.fitme.model.ProductVariant
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductVariantApiService {
    @GET("product-variants")
    fun getProductVariants(): Call<BaseResponse<List<ProductVariant>>>

    @GET("product-variants/{id}")
    fun getProductVariantById(@Path("id") id: String): Call<BaseResponse<ProductVariant>>
}
