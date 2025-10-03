package com.pbl6.fitme.session

import android.content.Context
import com.pbl6.fitme.network.LoginResponse
import hoang.dqm.codebase.utils.pref.SpUtils
import java.text.SimpleDateFormat
import java.util.*

class SessionManager private constructor() {

    companion object {
        private const val KEY_LOGIN_RESPONSE = "login_response"

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

    fun getLoginResponse(context: Context): LoginResponse? {
        return spUtils.getData(context, KEY_LOGIN_RESPONSE, LoginResponse::class.java)
    }

    fun getAccessToken(context: Context): String? {
        return getLoginResponse(context)?.result?.token
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