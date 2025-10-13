package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.network.AuthApiService
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.LoginRequest
import com.pbl6.fitme.network.LoginResponse
import com.pbl6.fitme.network.RegisterRequest
import com.pbl6.fitme.network.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthRepository {
    private var token: String? = null
    private val authApi = ApiClient.getRetrofit(token).create(AuthApiService::class.java)

    fun login(email: String, password: String, onResult: (LoginResponse?) -> Unit) {
        val request = LoginRequest(email, password)
        authApi.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("AuthRepository", "API error: ${response.code()} - ${response.message()}")
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("AuthRepository", "Network failure: ${t.localizedMessage}", t)
                onResult(null)
            }
        })
    }
    fun register(request: RegisterRequest, onResult: (RegisterResponse?) -> Unit) {
        authApi.register(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("AuthRepository", "API error: ${response.code()} - ${response.message()}")
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("AuthRepository", "Network failure: ${t.localizedMessage}", t)
                onResult(null)
            }
        })
    }
}
