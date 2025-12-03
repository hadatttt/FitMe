package com.pbl6.fitme.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.TryOnApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TryOnRepository {
    private val api = ApiClient.retrofit.create(TryOnApiService::class.java)

    fun virtualTryOn(context: Context, person: Bitmap, cloth: Bitmap, onResult: (ByteArray?) -> Unit) {
        try {
            val personBytes = bitmapToJpeg(person)
            val clothBytes = bitmapToJpeg(cloth)

            val personReq = personBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val clothReq = clothBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

            val personPart = MultipartBody.Part.createFormData("person", "person.jpg", personReq)
            val clothPart = MultipartBody.Part.createFormData("cloth", "cloth.jpg", clothReq)

            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(context)
            val authHeader: String? = if (!token.isNullOrBlank()) "Bearer $token" else null
            api.virtualTryOn(authHeader, personPart, clothPart).enqueue(object : Callback<okhttp3.ResponseBody> {
                override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                    if (response.isSuccessful) {
                        try {
                            val bytes = response.body()?.bytes()
                            onResult(bytes)
                        } catch (ex: Exception) {
                            Log.e("TryOnRepo", "Failed to read tryon bytes", ex)
                            onResult(null)
                        }
                    } else {
                        Log.e("TryOnRepo", "TryOn API failed: ${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                    Log.e("TryOnRepo", "TryOn network failure", t)
                    onResult(null)
                }
            })
        } catch (ex: Exception) {
            Log.e("TryOnRepo", "virtualTryOn failed", ex)
            onResult(null)
        }
    }

    private fun bitmapToJpeg(bmp: Bitmap): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        return baos.toByteArray()
    }
}
