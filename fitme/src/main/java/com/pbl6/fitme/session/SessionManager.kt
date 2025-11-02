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
    private const val KEY_RECIPIENT_NAME = "session_recipient_name"

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
        // TODO: if LoginResponse.result contains user id, parse and return it here.
        return null
    }
    fun getRefreshToken(context: Context): String? {
        return getLoginResponse(context)?.result?.refreshToken
    }

    fun clearSession(context: Context) {
        spUtils.removeKey(context, KEY_LOGIN_RESPONSE)
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