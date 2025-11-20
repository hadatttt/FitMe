package com.pbl6.fitme.session

import android.content.Context
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
    private const val KEY_CART_ID = "session_cart_id"
    private const val KEY_PERSISTENT_CART_ID = "persistent_cart_id"
    private const val KEY_LOCAL_CART = "local_cart_items"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }

    private val spUtils = SpUtils.getDefaultInstance()

    fun saveLoginResponse(context: Context, loginResponse: LoginResponse) {
        spUtils.saveData(context, loginResponse, KEY_LOGIN_RESPONSE)
    }

    fun saveUserEmail(context: Context, email: String) {
        spUtils.saveData(context, email, KEY_USER_EMAIL)
    }

    fun saveRecipientName(context: Context, name: String) {
        spUtils.saveData(context, name, KEY_RECIPIENT_NAME)
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

    fun getAccessToken(context: Context): String? {
        return getLoginResponse(context)?.result?.token
    }

    /**
     * Returns the current user's id if available. The login response stored does not
     * include user id in this project, so this method returns null by default.
     * If your backend returns user id inside the login payload, update this to
     * extract and return it.
     */
    fun getUserId(context: Context): java.util.UUID? {
        // First try stored explicit user id (saved after registration or explicit save)
        try {
            val stored = spUtils.getData(context, KEY_USER_ID, String::class.java)
            android.util.Log.d("SessionManager", "getUserId: stored=$stored")
            if (stored != null) {
                return try {
                    val uuid = java.util.UUID.fromString(stored)
                    android.util.Log.d("SessionManager", "getUserId: returning stored UUID=$uuid")
                    uuid
                } catch (ex: Exception) {
                    android.util.Log.w("SessionManager", "getUserId: stored value is not a valid UUID: $stored", ex)
                    null
                }
            }
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "getUserId: error reading stored userId", ex)
            ex.printStackTrace()
        }

        // Fallback: try to extract from stored login token (JWT payload)
        val token = getLoginResponse(context)?.result?.token ?: run {
            android.util.Log.w("SessionManager", "getUserId: no stored userId and no login token found")
            return null
        }
        try {
            val parts = token.split('.')
            if (parts.size < 2) {
                android.util.Log.w("SessionManager", "getUserId: token does not have 3 parts")
                return null
            }
            val payload = parts[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val json = String(decoded, Charsets.UTF_8)
            val obj = org.json.JSONObject(json)
            val candidates = listOf("userId", "user_id", "sub", "id")
            for (k in candidates) {
                if (obj.has(k)) {
                    val v = obj.get(k).toString()
                    android.util.Log.d("SessionManager", "getUserId: found JWT claim $k=$v")
                    return try {
                        val uuid = java.util.UUID.fromString(v)
                        android.util.Log.d("SessionManager", "getUserId: extracted UUID from JWT=$uuid")
                        uuid
                    } catch (ex: Exception) {
                        android.util.Log.w("SessionManager", "getUserId: JWT claim $k is not a valid UUID: $v", ex)
                        // some tokens store numeric or other id forms; not a UUID
                        null
                    }
                }
            }
            android.util.Log.w("SessionManager", "getUserId: no recognized userId claim found in JWT")
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "getUserId: error parsing JWT", ex)
        }
        return null
    }
    fun saveUserId(context: Context, userId: String) {
        try {
            spUtils.saveData(context, userId, KEY_USER_ID)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    fun getRefreshToken(context: Context): String? {
        return getLoginResponse(context)?.result?.refreshToken
    }

    // Local cart helpers: store cart items locally when backend cart is unavailable.
    fun addLocalCartItem(context: Context, variantId: java.util.UUID, quantity: Int) {
        try {
            val existing = spUtils.getData(context, KEY_LOCAL_CART, Array<com.pbl6.fitme.model.CartItem>::class.java)
            val list = existing?.toMutableList() ?: mutableListOf()
            val cartItem = com.pbl6.fitme.model.CartItem(
                cartItemId = java.util.UUID.randomUUID(),
                addedAt = java.time.OffsetDateTime.now().toString(),
                quantity = quantity,
                cartId = java.util.UUID.randomUUID(),
                variantId = variantId
            )
            list.add(cartItem)
            // Save back as array to SpUtils
            spUtils.saveData(context, list.toTypedArray(), KEY_LOCAL_CART)
            android.util.Log.d("SessionManager", "addLocalCartItem: saved item variant=$variantId qty=$quantity")
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "addLocalCartItem failed", ex)
        }
    }

    fun getLocalCartItems(context: Context): List<com.pbl6.fitme.model.CartItem>? {
        return try {
            val arr = spUtils.getData(context, KEY_LOCAL_CART, Array<com.pbl6.fitme.model.CartItem>::class.java)
            arr?.toList()
        } catch (ex: Exception) {
            android.util.Log.e("SessionManager", "getLocalCartItems failed", ex)
            null
        }
    }

    fun clearSession(context: Context) {
        spUtils.removeKey(context, KEY_LOGIN_RESPONSE)
    }

    // Cart ID helpers: store/retrieve a persistent cartId for server-side cart operations
    fun getOrCreateCartId(context: Context): java.util.UUID {
        return try {
            // First, try to get the persistent cart ID from server (saved during registration)
            val persistent = spUtils.getData(context, KEY_PERSISTENT_CART_ID, String::class.java)
            if (persistent != null) {
                return java.util.UUID.fromString(persistent)
            }
            
            // Fall back to session cart ID
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

    // Save persistent cart ID returned from server (after registration)
    fun savePersistentCartId(context: Context, cartId: java.util.UUID) {
        try {
            spUtils.saveData(context, cartId.toString(), KEY_PERSISTENT_CART_ID)
            android.util.Log.d("SessionManager", "Persistent cart ID saved: $cartId")
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