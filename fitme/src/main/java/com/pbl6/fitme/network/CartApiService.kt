package com.pbl6.fitme.network

import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.model.AddCartRequest
import retrofit2.Call
import retrofit2.http.*

data class ShoppingCartResponse(
    val cartId: String,
    val userId: String,
    val items: List<CartItem>
)

interface CartApiService {

    // Create shopping cart for new user (called after registration)
    @POST("cart-items/create-for-user/{userId}")
    fun createCartForUser(
        @Path("userId") userId: String
    ): Call<ShoppingCartResponse>

    // Lấy danh sách item trong giỏ
    @GET("cart-items/{cartId}")
    fun getCartItems(
        @Header("Authorization") token: String,
        @Path("cartId") cartId: String
    ): Call<List<CartItem>>

    // Thêm hoặc cập nhật số lượng item
    @POST("cart-items/{cartId}")
    fun addOrUpdateCartItem(
        @Header("Authorization") token: String,
        @Path("cartId") cartId: String,
        @Body req: AddCartRequest
    ): Call<CartItem>

    // New endpoint: get cart by profile/user id (backend controller at /carts/user/{userId})
    @GET("carts/user/{userId}")
    fun getCartByUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Call<ShoppingCartResponse>
    // Xóa item trong giỏ
    @DELETE("cart-items/{cartItemId}")
    fun removeCartItem(
        @Header("Authorization") token: String,
        @Path("cartItemId") cartItemId: String
    ): Call<Void>

}
