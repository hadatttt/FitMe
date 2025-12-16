package com.pbl6.fitme.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface RecommendApiService {
    @GET("recommend/{userId}")
    fun getRecommendations(
        @Header("Authorization") auth: String?,
        @Path("userId") userId: String,
        @Query("n") n: Int = 10,
        @Query("season") season: String? = null,
        @Query("model") model: String = "hybrid"
    ): Call<List<com.pbl6.fitme.model.Product>>
}
