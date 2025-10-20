package com.pbl6.fitme.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class LogoutRequest(
    val token: String
)
data class LogoutResponse(
    val code: Int,
    val message: String,
    val result: String?
)
interface AuthApiService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<com.pbl6.fitme.network.LoginResponse>
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
    @POST("auth/logout")
    fun logout(@Body request: LogoutRequest): Call<LogoutResponse>
}
