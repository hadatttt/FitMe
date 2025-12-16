package com.pbl6.fitme.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.Log
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.TryOnApiService
import com.pbl6.fitme.session.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class TryOnRepository {
    private val api = ApiClient.retrofit.create(TryOnApiService::class.java)

    fun virtualTryOn(context: Context, person: Bitmap, cloth: Bitmap, onResult: (ByteArray?, String?) -> Unit) {
        try {
            // 1. Resize ảnh
            val resizedPerson = resizeBitmap(person, 1024)
            val resizedCloth = resizeBitmap(cloth, 1024)

            // 2. Convert sang JPEG (Đã thêm fix lỗi Hardware Bitmap)
            val personBytes = convertBitmapToJpeg(resizedPerson, 80)
            val clothBytes = convertBitmapToJpeg(resizedCloth, 80)

            // ... (Phần code API giữ nguyên như cũ) ...
            val reqType = "image/jpeg".toMediaTypeOrNull()
            val personReq = personBytes.toRequestBody(reqType)
            val clothReq = clothBytes.toRequestBody(reqType)

            val personPart = MultipartBody.Part.createFormData("person", "person.jpg", personReq)
            val clothPart = MultipartBody.Part.createFormData("cloth", "cloth.jpg", clothReq)

            val token = SessionManager.getInstance().getAccessToken(context)
            val authHeader: String? = if (!token.isNullOrBlank()) "Bearer $token" else null

            api.virtualTryOn(authHeader, personPart, clothPart).enqueue(object : Callback<okhttp3.ResponseBody> {
                override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                    if (response.isSuccessful) {
                        try {
                            val bytes = response.body()?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                onResult(bytes, null)
                            } else {
                                onResult(null, "Server trả về dữ liệu rỗng")
                            }
                        } catch (ex: Exception) {
                            onResult(null, "Lỗi đọc dữ liệu: ${ex.message}")
                        }
                    } else {
                        val errorMsg = "API Lỗi ${response.code()}: ${response.message()}"
                        Log.e("TryOnRepo", errorMsg)
                        onResult(null, errorMsg)
                    }
                }

                override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                    val errorMsg = "Lỗi kết nối: ${t.localizedMessage}"
                    Log.e("TryOnRepo", errorMsg, t)
                    onResult(null, errorMsg)
                }
            })

        } catch (ex: Exception) {
            val errorMsg = "Lỗi xử lý ảnh: ${ex.message}"
            Log.e("TryOnRepo", errorMsg, ex)
            onResult(null, errorMsg)
        }
    }

    // --- CÁC HÀM HELPER ĐÃ SỬA ---

    private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        try {
            // FIX: Chuyển Hardware Bitmap sang Software trước khi resize để tránh lỗi
            val safeSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }

            if (safeSource.height >= safeSource.width) {
                if (safeSource.height <= maxLength) return safeSource
                val aspectRatio = safeSource.width.toDouble() / safeSource.height.toDouble()
                val targetWidth = (maxLength * aspectRatio).toInt()
                return Bitmap.createScaledBitmap(safeSource, targetWidth, maxLength, false)
            } else {
                if (safeSource.width <= maxLength) return safeSource
                val aspectRatio = safeSource.height.toDouble() / safeSource.width.toDouble()
                val targetHeight = (maxLength * aspectRatio).toInt()
                return Bitmap.createScaledBitmap(safeSource, maxLength, targetHeight, false)
            }
        } catch (e: Exception) {
            return source
        }
    }

    private fun convertBitmapToJpeg(bmp: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()

        // FIX QUAN TRỌNG: Kiểm tra Hardware Bitmap
        // Nếu là Hardware Bitmap (không vẽ được), copy sang ARGB_8888 (Software)
        val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bmp
        }

        // Tạo canvas nền trắng để xử lý ảnh PNG trong suốt
        val newBitmap = Bitmap.createBitmap(safeBitmap.width, safeBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(safeBitmap, 0f, 0f, null)

        newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        // Recycle để giải phóng bộ nhớ (trừ bitmap gốc truyền vào)
        if (newBitmap != safeBitmap) newBitmap.recycle()
        if (safeBitmap != bmp) safeBitmap.recycle() // Chỉ recycle nếu nó là bản copy

        return stream.toByteArray()
    }
}