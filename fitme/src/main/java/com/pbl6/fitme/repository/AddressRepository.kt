package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.ShippingAddress
import com.pbl6.fitme.network.AddressApiService
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.UserAddressRequest
import com.pbl6.fitme.network.UserAddressResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddressRepository {

    private val addressApiService = ApiClient.retrofit.create(AddressApiService::class.java)

    fun addUserAddress(token: String, request: UserAddressRequest, onResult: (UserAddressResponse?) -> Unit) {
        val bearerToken = "Bearer $token"
        addressApiService.addUserAddress(bearerToken, request).enqueue(object : Callback<UserAddressResponse> {
            override fun onResponse(call: Call<UserAddressResponse>, response: Response<UserAddressResponse>) {
                if (response.isSuccessful) onResult(response.body()) else onResult(null)
            }
            override fun onFailure(call: Call<UserAddressResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }
    fun getUserAddresses(token: String, email: String, onResult: (List<ShippingAddress>?) -> Unit) {
        val bearerToken = "Bearer $token"

        addressApiService.getUserAddresses(bearerToken, email).enqueue(object : Callback<List<ShippingAddress>> {
            override fun onResponse(
                call: Call<List<ShippingAddress>>,
                response: Response<List<ShippingAddress>>
            ) {
                if (response.isSuccessful) {
                    Log.d("AddressRepo", "Get addresses success: ${response.body()?.size} items")
                    onResult(response.body())
                } else {
                    Log.e("AddressRepo", "Get addresses failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<ShippingAddress>>, t: Throwable) {
                Log.e("AddressRepo", "Network error: ${t.localizedMessage}")
                onResult(null)
            }
        })
    }
}