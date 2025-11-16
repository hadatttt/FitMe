package com.pbl6.fitme.network

import com.pbl6.fitme.model.UserResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface UserApiService {
    @GET("users/user-profile/{email}")
    fun getUserProfileByEmail(@Header("Authorization") bearer: String, @Path("email") email: String): Call<BaseResponse<UserResponse>>
}
