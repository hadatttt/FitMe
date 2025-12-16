package com.pbl6.fitme.network

import com.pbl6.fitme.model.UserResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PartMap
import java.io.File

interface UserApiService {

    @Multipart
    @PUT("users/{userId}")
    fun updateUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part avatar: MultipartBody.Part? = null
    ): Call<BaseResponse<UserResponse>>

    @GET("users/{userId}")
    fun getUserDetail(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Call<UserDetailResponse>

    @GET("users/user-profile/{email}")
    fun getUserProfileByEmail(
        @Header("Authorization") token: String,
        @Path("email") email: String
    ): Call<BaseResponse<UserResponse>>
}
data class UpdateUserRequest( val username: String, val password: String, val email: String, val fullName: String, val dateOfBirth: String, val phone: String, val avatar: String? = null, val avatarUrl: String? = null, val gender: String? = null, val roleIds: List<String> )

