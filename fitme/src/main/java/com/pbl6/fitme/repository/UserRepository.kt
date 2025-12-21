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
    fun getUserPoints(token: String, userId: String, onResult: (Int?) -> Unit) {
        val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        Log.d("UserRepo", ">>> Requesting Points for UserID: $userId")

        // QUAN TRỌNG: Đổi <Int> thành <Double> để nhận số lẻ từ Backend
        userApiService.getUserPoints(bearerToken, userId).enqueue(object : Callback<BaseResponse<Double>> {
            override fun onResponse(
                call: Call<BaseResponse<Double>>,
                response: Response<BaseResponse<Double>>
            ) {
                Log.d("UserRepo", ">>> HTTP Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()

                    Log.d("UserRepo", ">>> API Code: ${apiResponse?.code}")
                    Log.d("UserRepo", ">>> API Result (Raw Coin): ${apiResponse?.result}")

                    // Chấp nhận code 200, 0 hoặc 1000
                    if (apiResponse?.code == 200 || apiResponse?.code == 0 || apiResponse?.code == 1000) {

                        // 1. Lấy giá trị Coin gốc (Double), nếu null thì bằng 0.0
                        val rawCoin: Double = apiResponse?.result ?: 0.0

                        // 2. Nhân với 25.000
                        val calculatedVal = rawCoin * 25000

                        // 3. Quy tròn (round) và chuyển về Int
                        val finalPoints = kotlin.math.round(calculatedVal).toInt()

                        Log.d("UserRepo", ">>> Converted: $rawCoin * 25000 = $finalPoints points")
                        onResult(finalPoints)
                    } else {
                        Log.e("UserRepo", ">>> API Logic Error: Code is not 200/0/1000")
                        onResult(0)
                    }
                } else {
                    // Xử lý lỗi HTTP
                    try {
                        val errorStr = response.errorBody()?.string()
                        Log.e("UserRepo", ">>> HTTP Error Body: $errorStr")
                    } catch (e: Exception) {
                        Log.e("UserRepo", ">>> HTTP Error (Cannot read body)")
                    }
                    onResult(0)
                }
            }

            override fun onFailure(call: Call<BaseResponse<Double>>, t: Throwable) {
                Log.e("UserRepo", ">>> NETWORK FAILURE: ${t.message}")
                t.printStackTrace()
                onResult(0)
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
