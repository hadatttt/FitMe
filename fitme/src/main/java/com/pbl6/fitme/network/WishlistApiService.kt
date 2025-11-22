package com.pbl6.fitme.network

import com.pbl6.fitme.model.AddWishlistRequest
import com.pbl6.fitme.model.WishlistDto
import com.pbl6.fitme.model.WishlistItem
import com.pbl6.fitme.model.WishlistRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

data class WishlistResponse(
    val wishlistId: String,
    val name: String,
    val userId: String,
    val createdAt: String,
    val items: List<WishlistItemResponse>?
)
data class WishlistItemResponse(
    val wishlistItemId: String,
    val productId: String,
    val addedAt: String
)


interface WishlistApiService {
    @POST("wishlists")
    fun createWishlist(
        @Header("user-id") userId: String, // ✔ đúng
        @Header("Authorization") bearer: String, // ✔ đúng header chuẩn
        @Body request: WishlistRequest
    ): Call<WishlistResponse>

    @GET("wishlists/user/{userId}")
    fun getWishlistsByUser(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): Call<List<WishlistDto>>

    @POST("wishlists/{wishlistId}/items")
    fun addItemToWishlist(
        @Header("Authorization") bearer: String,
        @Path("wishlistId") wishlistId: String,
        @Body req: AddWishlistRequest
    ): Call<Void>

    @GET("wishlists/{wishlistId}/items")
    fun getWishlistItems(
        @Header("Authorization") bearer: String,
        @Path("wishlistId") wishlistId: String
    ): Call<List<WishlistItem>>

    @DELETE("wishlists/items/{wishlistItemId}")
    fun removeWishlistItem(
        @Header("Authorization") bearer: String,
        @Path("wishlistItemId") wishlistItemId: String
    ): Call<Void>
}
