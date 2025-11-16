package com.pbl6.fitme.repository

import com.pbl6.fitme.network.CartApiService
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.network.ApiClient
import retrofit2.Call
import com.pbl6.fitme.network.BaseResponse
import retrofit2.Callback
import retrofit2.Response

class CartRepository {
    private var token: String? = null
    private val cartApiService = ApiClient.retrofit.create(CartApiService::class.java)

    fun getCart(token: String, cartId: String, onResult: (List<CartItem>?) -> Unit) {
        val bearerToken = "Bearer $token"
        cartApiService.getCartItems(bearerToken, cartId).enqueue(object : Callback<List<CartItem>> {
            override fun onResponse(call: Call<List<CartItem>>, response: Response<List<CartItem>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<CartItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
