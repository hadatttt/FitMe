package com.pbl6.fitme.network

import com.pbl6.fitme.model.ProductImage
import retrofit2.Call
import retrofit2.http.GET

interface ProductImageApiService {
    @GET("product-images")
    fun getProductImages(): Call<List<ProductImage>>
}
