package com.pbl6.fitme.network

import com.pbl6.fitme.model.WishlistItem
import com.pbl6.fitme.model.AddWishlistRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header

interface WishlistApiService {
    @GET("wishlist/items")
    fun getWishlistItems(): Call<List<WishlistItem>>

    @GET("wishlist/items/{id}")
    fun getWishlistItem(@Path("id") id: String): Call<WishlistItem>

    @POST("wishlist/items")
    fun addToWishlist(@Header("Authorization") token: String, @Body req: AddWishlistRequest): Call<Void>
}
