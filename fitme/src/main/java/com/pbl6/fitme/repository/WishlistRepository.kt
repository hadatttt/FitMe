package com.pbl6.fitme.repository

import com.pbl6.fitme.network.WishlistApiService
import com.pbl6.fitme.model.WishlistItem
import com.pbl6.fitme.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WishlistRepository {
    private var token: String? = null
    private val wishlistApiService = ApiClient.retrofit.create(WishlistApiService::class.java)

    fun getWishlist(onResult: (List<WishlistItem>?) -> Unit) {
        wishlistApiService.getWishlistItems().enqueue(object : Callback<List<WishlistItem>> {
            override fun onResponse(call: Call<List<WishlistItem>>, response: Response<List<WishlistItem>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
