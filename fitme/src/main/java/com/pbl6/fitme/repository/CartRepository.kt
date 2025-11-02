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

    fun getCart(onResult: (List<CartItem>?) -> Unit) {
        cartApiService.getCartItems().enqueue(object : Callback<BaseResponse<List<CartItem>>> {
            override fun onResponse(call: Call<BaseResponse<List<CartItem>>>, response: Response<BaseResponse<List<CartItem>>>) {
                onResult(response.body()?.result)
            }
            override fun onFailure(call: Call<BaseResponse<List<CartItem>>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
