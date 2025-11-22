package com.pbl6.fitme.repository

import com.pbl6.fitme.network.WishlistApiService
import com.pbl6.fitme.model.WishlistItem
import com.pbl6.fitme.model.WishlistDto
import android.util.Log
import org.json.JSONObject
import com.pbl6.fitme.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WishlistRepository {
    private var token: String? = null
    private val wishlistApiService = ApiClient.retrofit.create(WishlistApiService::class.java)

    /**
     * Fetch wishlist items for the currently authenticated user. Requires the raw JWT token
     * so we can extract user id claim and call server endpoints.
     */
    fun getWishlist(token: String?, onResult: (List<WishlistItem>?) -> Unit) {
        if (token == null) {
            onResult(null)
            return
        }

        fun extractUserIdFromJwt(jwt: String?): String? {
            if (jwt == null) return null
            try {
                val parts = jwt.split('.')
                if (parts.size < 2) return null
                val payload = parts[1]
                val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val json = String(decoded, Charsets.UTF_8)
                val obj = JSONObject(json)
                val candidates = listOf("userId", "user_id", "sub", "id")
                for (k in candidates) {
                    if (obj.has(k)) return obj.get(k).toString()
                }
            } catch (ex: Exception) {
                Log.e("WishlistRepository", "extractUserIdFromJwt failed", ex)
            }
            return null
        }

        val userId = extractUserIdFromJwt(token)
        if (userId == null) {
            onResult(null)
            return
        }

        val bearer = "Bearer $token"
        wishlistApiService.getWishlistsByUser(bearer, userId).enqueue(object : Callback<List<WishlistDto>> {
            override fun onResponse(call: Call<List<WishlistDto>>, response: Response<List<WishlistDto>>) {
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    if (list.isNotEmpty()) {
                        val wishlistId = list[0].wishlistId.toString()
                        wishlistApiService.getWishlistItems(bearer, wishlistId).enqueue(object : Callback<List<WishlistItem>> {
                            override fun onResponse(call: Call<List<WishlistItem>>, response: Response<List<WishlistItem>>) {
                                if (response.isSuccessful) {
                                    onResult(response.body())
                                } else {
                                    onResult(null)
                                }
                            }

                            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                                onResult(null)
                            }
                        })
                    } else {
                        onResult(emptyList())
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<WishlistDto>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    /**
     * Fetch wishlist items for the supplied profileId (explicit), bypassing JWT claim extraction.
     */
    fun getWishlistByProfile(token: String?, profileId: String?, onResult: (List<WishlistItem>?) -> Unit) {
        if (token == null || profileId.isNullOrBlank()) {
            onResult(null)
            return
        }

        val bearer = "Bearer $token"
        wishlistApiService.getWishlistsByUser(bearer, profileId).enqueue(object : Callback<List<WishlistDto>> {
            override fun onResponse(call: Call<List<WishlistDto>>, response: Response<List<WishlistDto>>) {
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    if (list.isNotEmpty()) {
                        val wishlistId = list[0].wishlistId.toString()
                        wishlistApiService.getWishlistItems(bearer, wishlistId).enqueue(object : Callback<List<WishlistItem>> {
                            override fun onResponse(call: Call<List<WishlistItem>>, response: Response<List<WishlistItem>>) {
                                if (response.isSuccessful) {
                                    onResult(response.body())
                                } else {
                                    onResult(null)
                                }
                            }

                            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                                onResult(null)
                            }
                        })
                    } else {
                        onResult(emptyList())
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<WishlistDto>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    /**
     * Remove a wishlist item by wishlistItemId. Calls callback with true on success.
     */
    fun removeWishlistItem(token: String?, wishlistItemId: String, onResult: (Boolean) -> Unit) {
        if (token == null) {
            onResult(false)
            return
        }
        val bearer = "Bearer $token"
        wishlistApiService.removeWishlistItem(bearer, wishlistItemId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                onResult(false)
            }
        })
    }
}
