package com.pbl6.fitme.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AddressApiService {

    @POST("user-addresses")
    fun addUserAddress(
        @Header("Authorization") token: String,
        @Body request: UserAddressRequest
    ): Call<UserAddressResponse>

    @GET("user-addresses")
    fun getUserAddresses(
        @Header("Authorization") token: String,
        @Query("email") email: String
    ): Call<List<UserAddressResponse>>
}