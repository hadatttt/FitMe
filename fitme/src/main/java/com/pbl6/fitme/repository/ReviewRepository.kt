package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.Review
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.CreateReviewRequest
import com.pbl6.fitme.network.ReviewApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewRepository {

    private val api = ApiClient.retrofit.create(ReviewApiService::class.java)

    // GET reviews by product
    fun getReviewsByProduct(token: String, productId: String, onResult: (List<Review>?) -> Unit) {
        val bearerToken = "Bearer $token"

        api.getReviewsByProduct(bearerToken, productId)
            .enqueue(object : Callback<List<Review>> {
                override fun onResponse(
                    call: Call<List<Review>>,
                    response: Response<List<Review>>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        Log.e("ReviewRepository", "getReviews failed code=${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<List<Review>>, t: Throwable) {
                    Log.e("ReviewRepository", "getReviews failed", t)
                    onResult(null)
                }
            })
    }

    fun createReview(
        userEmail: String,
        productId: String,
        rating: Int,
        comment: String,
        onResult: (Review?) -> Unit
    ) {
        val request = CreateReviewRequest(
            productId = productId,
            rating = rating,
            comment = comment
        )

        api.createReview(userEmail, request)
            .enqueue(object : Callback<Review> {
                override fun onResponse(call: Call<Review>, response: Response<Review>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        Log.e("ReviewRepository", "createReview failed code=${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<Review>, t: Throwable) {
                    Log.e("ReviewRepository", "createReview network error", t)
                    onResult(null)
                }
            })
    }


}
