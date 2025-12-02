package com.pbl6.fitme.network

import com.pbl6.fitme.model.ProductResponse
import com.pbl6.fitme.model.Review
import retrofit2.Call
import retrofit2.http.*

interface ReviewApiService {

    @GET("reviews/product/{productId}")
    fun getReviewsByProduct(
        @Header("Authorization") token: String,
        @Path("productId") productId: String
    ): Call<List<Review>>
    @POST("reviews")
    fun createReview(
        @Header("user-email") userEmail: String,
        @Body request: CreateReviewRequest
    ): Call<Review>
}
data class CreateReviewRequest(
    val productId: String,
    val rating: Int,
    val comment: String
)
