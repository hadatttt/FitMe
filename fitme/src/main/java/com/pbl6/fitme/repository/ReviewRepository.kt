package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.Review
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.BaseResponse
import com.pbl6.fitme.network.ReviewApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewRepository {

    private val reviewApi = ApiClient.retrofit.create(ReviewApiService::class.java)

    fun getReviewsByProduct(token: String, productId: String, onResult: (List<Review>?) -> Unit) {
        val bearerToken = "Bearer $token"

        reviewApi.getReviewsByProduct(bearerToken, productId)
            .enqueue(object : Callback<List<Review>> { // <--- ĐÃ SỬA
                override fun onResponse(
                    call: Call<List<Review>>, // <--- ĐÃ SỬA
                    response: Response<List<Review>> // <--- ĐÃ SỬA
                ) {
                    if (response.isSuccessful) {
                        // Trả về thẳng response.body() vì nó đã là List<Review>
                        onResult(response.body()) // <--- ĐÃ SỬA
                    } else {
                        Log.e("ReviewRepository", "getReviewsByProduct failed code=${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<List<Review>>, t: Throwable) { // <--- ĐÃ SỬA
                    Log.e("ReviewRepository", "getReviewsByProduct network failure", t)
                    onResult(null)
                }
            })
    }

}