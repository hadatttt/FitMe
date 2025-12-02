package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.UserResponse

import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.BaseResponse
import com.pbl6.fitme.network.UserApiService
import com.pbl6.fitme.network.UserDetailResponse
import com.pbl6.fitme.network.UserResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

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
    fun updateUser(
        token: String,
        userId: String,
        params: Map<String, RequestBody>,
        avatarPart: MultipartBody.Part? = null,
        onResult: (UserResponse?) -> Unit
    ) {
        val bearerToken = "Bearer $token"

        userApiService.updateUser(bearerToken, userId, params, avatarPart)
            .enqueue(object : Callback<BaseResponse<UserResponse>> {
                override fun onResponse(
                    call: Call<BaseResponse<UserResponse>>,
                    response: Response<BaseResponse<UserResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        onResult(response.body()?.result)
                    } else {
                        Log.e("UserRepository", "Update API Error: ${response.code()} - ${response.message()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<UserResponse>>, t: Throwable) {
                    Log.e("UserRepository", "Network failure: ${t.localizedMessage}")
                    onResult(null)
                }
            })
    }

    fun createUpdateUserParts(
        username: String,
        password: String,
        email: String,
        fullName: String,
        dateOfBirth: String,
        phone: String,
        roleIds: List<String>,
        avatarFile: File? = null
    ): Pair<Map<String, RequestBody>, MultipartBody.Part?> {

        val params = HashMap<String, RequestBody>()
        params["username"] = username.toRequestBody("text/plain".toMediaType())
        params["password"] = password.toRequestBody("text/plain".toMediaType())
        params["email"] = email.toRequestBody("text/plain".toMediaType())
        params["fullName"] = fullName.toRequestBody("text/plain".toMediaType())
        params["dateOfBirth"] = dateOfBirth.toRequestBody("text/plain".toMediaType())
        params["phone"] = phone.toRequestBody("text/plain".toMediaType())
        params["roleIds"] = roleIds.joinToString(",").toRequestBody("text/plain".toMediaType())

        val avatarPart = avatarFile?.let {
            val requestFile = it.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("avatar", it.name, requestFile)
        }

        return Pair(params, avatarPart)
    }
}
