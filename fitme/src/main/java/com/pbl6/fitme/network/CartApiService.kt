package com.pbl6.fitme.network

import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.model.AddCartRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header

interface CartApiService {
    @GET("cart/items")
    fun getCartItems(): Call<BaseResponse<List<CartItem>>>

    @GET("cart/items/{id}")
    fun getCartItem(@Path("id") id: String): Call<BaseResponse<CartItem>>

    @POST("cart/items")
    fun addToCart(@Header("Authorization") token: String, @Body req: AddCartRequest): Call<Void>
}
