package com.pbl6.fitme.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)

interface AuthApiService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<com.pbl6.fitme.network.LoginResponse>
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
}
