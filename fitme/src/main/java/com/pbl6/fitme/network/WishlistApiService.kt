package com.pbl6.fitme.network

import com.pbl6.fitme.model.WishlistItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WishlistApiService {
    @GET("wishlist/items")
    fun getWishlistItems(): Call<List<WishlistItem>>

    @GET("wishlist/items/{id}")
    fun getWishlistItem(@Path("id") id: String): Call<WishlistItem>
}
