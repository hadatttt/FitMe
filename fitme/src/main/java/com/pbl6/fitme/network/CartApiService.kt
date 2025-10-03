package com.pbl6.fitme.network

import com.pbl6.fitme.model.CartItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CartApiService {
    @GET("cart/items")
    fun getCartItems(): Call<List<CartItem>>

    @GET("cart/items/{id}")
    fun getCartItem(@Path("id") id: String): Call<CartItem>
}
