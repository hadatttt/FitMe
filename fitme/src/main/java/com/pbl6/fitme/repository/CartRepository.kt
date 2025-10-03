package com.pbl6.fitme.repository

import com.pbl6.fitme.network.CartApiService
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartRepository {
    private val cartApiService = ApiClient.retrofit.create(CartApiService::class.java)

    fun getCart(onResult: (List<CartItem>?) -> Unit) {
        cartApiService.getCartItems().enqueue(object : Callback<List<CartItem>> {
            override fun onResponse(call: Call<List<CartItem>>, response: Response<List<CartItem>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<CartItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
