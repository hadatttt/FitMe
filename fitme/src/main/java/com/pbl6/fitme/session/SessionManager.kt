package com.pbl6.fitme.session

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.network.LoginResponse
import hoang.dqm.codebase.utils.pref.SpUtils
import java.text.SimpleDateFormat
import java.util.*

class SessionManager private constructor() {

    companion object {
        private const val KEY_LOGIN_RESPONSE = "login_response"
        private const val KEY_USER_EMAIL = "session_user_email"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_RECIPIENT_NAME = "session_recipient_name"
        private const val KEY_RECIPIENT_PHONE = "session_recipient_phone"
        private const val KEY_CART_ID = "session_cart_id"
        private const val KEY_PERSISTENT_CART_ID = "persistent_cart_id"

        // Key lưu trữ giỏ hàng dưới dạng JSON String
        private const val KEY_LOCAL_CART = "local_cart_items_json"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }

    private val spUtils = SpUtils.getDefaultInstance()
    private val gson = Gson() // Khởi tạo Gson

    // ... (Giữ nguyên các hàm save/get Login, Email, Recipient cũ) ...

    fun saveLoginResponse(context: Context, loginResponse: LoginResponse) {
        spUtils.saveData(context, loginResponse, KEY_LOGIN_RESPONSE)
    }

    fun saveUserEmail(context: Context, email: String) {
        spUtils.saveData(context, email, KEY_USER_EMAIL)
    }

    fun saveRecipientName(context: Context, name: String) {
        spUtils.saveData(context, name, KEY_RECIPIENT_NAME)
    }

    fun saveRecipientPhone(context: Context, phone: String) {
        try {
            spUtils.saveData(context, phone, KEY_RECIPIENT_PHONE)
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "saveRecipientPhone failed", ex)
        }
    }

    fun getLoginResponse(context: Context): LoginResponse? {
        return spUtils.getData(context, KEY_LOGIN_RESPONSE, LoginResponse::class.java)
    }

    fun getUserEmail(context: Context): String? {
        return spUtils.getData(context, KEY_USER_EMAIL, String::class.java)
    }

    fun getRecipientName(context: Context): String? {
        return spUtils.getData(context, KEY_RECIPIENT_NAME, String::class.java)
    }

    fun getRecipientPhone(context: Context): String? {
        return try {
            spUtils.getData(context, KEY_RECIPIENT_PHONE, String::class.java)
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "getRecipientPhone failed", ex)
            null
        }
    }

    fun getAccessToken(context: Context): String? {
        return getLoginResponse(context)?.result?.token
    }

    fun getUserId(context: Context): java.util.UUID? {
        try {
            val stored = spUtils.getData(context, KEY_USER_ID, String::class.java)
            if (stored != null) return java.util.UUID.fromString(stored)
        } catch (ex: Exception) { ex.printStackTrace() }

        val token = getLoginResponse(context)?.result?.token ?: return null
        try {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payload = parts[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val json = String(decoded, Charsets.UTF_8)
            val obj = org.json.JSONObject(json)
            val candidates = listOf("userId", "user_id", "sub", "id")
            for (k in candidates) {
                if (obj.has(k)) {
                    val v = obj.get(k).toString()
                    return try { java.util.UUID.fromString(v) } catch (ex: Exception) { null }
                }
            }
        } catch (ex: Exception) { ex.printStackTrace() }
        return null
    }

    fun saveUserId(context: Context, userId: String) {
        try { spUtils.saveData(context, userId, KEY_USER_ID) } catch (ex: Exception) { ex.printStackTrace() }
    }

    fun getRefreshToken(context: Context): String? {
        return getLoginResponse(context)?.result?.refreshToken
    }

    // ========================================================================
    // LOGIC LOCAL CART (Offline Mode)
    // ========================================================================

    /**
     * Thêm sản phẩm vào giỏ hàng local.
     */
    fun addToCartLocal(context: Context, newItem: CartItem) {
        try {
            val currentList = getLocalCartItems(context).toMutableList()
            val existingItem = currentList.find { it.variantId == newItem.variantId }

            if (existingItem != null) {
                // Đã có -> Cộng dồn số lượng
                existingItem.quantity += newItem.quantity
                android.util.Log.d("SessionManager", "Updated local item qty: ${existingItem.quantity}")
            } else {
                // Chưa có -> Thêm mới
                currentList.add(newItem)
                android.util.Log.d("SessionManager", "Added new local item")
            }

            // Lưu lại
            saveLocalCartItems(context, currentList)

        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "addToCartLocal failed", ex)
        }
    }

    /**
     * [MỚI] Hàm này dùng để lưu đè danh sách cart (dùng khi Xóa hoặc Update số lượng)
     */
    fun saveLocalCartItems(context: Context, items: List<CartItem>) {
        try {
            val jsonString = gson.toJson(items)
            spUtils.saveData(context, jsonString, KEY_LOCAL_CART)
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "saveLocalCartItems failed", ex)
        }
    }

    /**
     * Lấy toàn bộ danh sách giỏ hàng local.
     */
    fun getLocalCartItems(context: Context): List<CartItem> {
        return try {
            val jsonString = spUtils.getData(context, KEY_LOCAL_CART, String::class.java)
            if (jsonString.isNullOrBlank()) {
                return emptyList()
            }
            val type = object : TypeToken<ArrayList<CartItem>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "getLocalCartItems failed", ex)
            emptyList()
        }
    }

    fun clearLocalCart(context: Context) {
        spUtils.removeKey(context, KEY_LOCAL_CART)
    }

    // ========================================================================

    fun clearSession(context: Context) {
        spUtils.removeKey(context, KEY_LOGIN_RESPONSE)
        // spUtils.removeKey(context, KEY_LOCAL_CART)
    }

    fun getOrCreateCartId(context: Context): java.util.UUID {
        return try {
            val persistent = spUtils.getData(context, KEY_PERSISTENT_CART_ID, String::class.java)
            if (persistent != null) {
                return java.util.UUID.fromString(persistent)
            }
            val stored = spUtils.getData(context, KEY_CART_ID, String::class.java)
            if (stored != null) {
                java.util.UUID.fromString(stored)
            } else {
                val newId = java.util.UUID.randomUUID()
                spUtils.saveData(context, newId.toString(), KEY_CART_ID)
                newId
            }
        } catch (ex: Exception) {
            val newId = java.util.UUID.randomUUID()
            spUtils.saveData(context, newId.toString(), KEY_CART_ID)
            newId
        }
    }

    fun savePersistentCartId(context: Context, cartId: java.util.UUID) {
        try {
            spUtils.saveData(context, cartId.toString(), KEY_PERSISTENT_CART_ID)
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "Failed to save persistent cart ID", ex)
        }
    }

    fun isAccessTokenExpired(context: Context): Boolean {
        val expiryTime = getLoginResponse(context)?.result?.expiryTime ?: return true
        return isExpired(expiryTime)
    }

    fun isRefreshTokenExpired(context: Context): Boolean {
        val refreshExpiry = getLoginResponse(context)?.result?.refreshTokenExpiryTime ?: return true
        return isExpired(refreshExpiry)
    }

    private fun isExpired(expiryTime: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val date = sdf.parse(expiryTime)
            val now = Date()
            date == null || now.after(date)
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }
}