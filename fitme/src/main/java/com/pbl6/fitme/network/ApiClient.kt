package com.pbl6.fitme.network


import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
    private const val BASE_URL = "http://10.123.72.90:8080/api/"

    // Token sẽ được truyền khi đăng nhập thành công
    fun getRetrofit(token: String? = null): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
        if (!token.isNullOrEmpty()) {
            clientBuilder.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    return chain.proceed(request)
                }
            })
        }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
