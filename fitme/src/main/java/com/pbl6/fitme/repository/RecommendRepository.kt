package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.RecommendApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendRepository {
    private val api = ApiClient.retrofit.create(RecommendApiService::class.java)

    fun getRecommendations(context: android.content.Context, userId: String, n: Int = 10, onResult: (List<com.pbl6.fitme.model.Product>?) -> Unit) {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(context)
        // Safely build the Authorization header; pass null to omit header when token missing
        val authHeader: String? = if (!token.isNullOrBlank()) "Bearer $token" else null
        api.getRecommendations(authHeader, userId, n).enqueue(object : Callback<List<com.pbl6.fitme.model.Product>> {
            override fun onResponse(call: Call<List<com.pbl6.fitme.model.Product>>, response: Response<List<com.pbl6.fitme.model.Product>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("RecommendRepo", "Failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<com.pbl6.fitme.model.Product>>, t: Throwable) {
                Log.e("RecommendRepo", "Network failure", t)
                onResult(null)
            }
        })
    }
}
