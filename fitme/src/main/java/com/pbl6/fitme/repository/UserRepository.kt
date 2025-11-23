package com.pbl6.fitme.repository

import android.util.Log

import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.UserApiService
import com.pbl6.fitme.network.UserDetailResponse
import com.pbl6.fitme.network.UserResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class UserRepository {

    private val userApiService = ApiClient.retrofit.create(UserApiService::class.java)

    fun getUserDetail(token: String, userId: String, onResult: (UserResult?) -> Unit) {
        val bearerToken = "Bearer $token"

        userApiService.getUserDetail(bearerToken, userId).enqueue(object : Callback<UserDetailResponse> {
            override fun onResponse(
                call: Call<UserDetailResponse>,
                response: Response<UserDetailResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()
                    // Bỏ điều kiện kiểm tra code, trả luôn result
                    Log.d("UserRepository", "User result: ${apiResponse?.result}")
                    onResult(apiResponse?.result)
                } else {
                    Log.e("UserRepository", "API Error: ${response.code()} - ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<UserDetailResponse>, t: Throwable) {
                Log.e("UserRepository", "Network failure: ${t.localizedMessage}")
                onResult(null)
            }
        })
    }
}
