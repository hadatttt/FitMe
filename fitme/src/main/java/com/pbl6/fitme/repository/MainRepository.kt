package com.pbl6.fitme.repository

import com.pbl6.fitme.model.*
import com.pbl6.fitme.network.ProductApiService
import com.pbl6.fitme.network.CategoryApiService
import com.pbl6.fitme.network.CartApiService
import com.pbl6.fitme.network.WishlistApiService
import com.pbl6.fitme.network.ProductVariantApiService
import com.pbl6.fitme.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainRepository {
    private val productApi = ApiClient.retrofit.create(ProductApiService::class.java)
    private val categoryApi = ApiClient.retrofit.create(CategoryApiService::class.java)
    private val cartApi = ApiClient.retrofit.create(CartApiService::class.java)
    private val wishlistApi = ApiClient.retrofit.create(WishlistApiService::class.java)
    private val variantApi = ApiClient.retrofit.create(ProductVariantApiService::class.java)
    fun getProductVariants(onResult: (List<ProductVariant>?) -> Unit) {
        variantApi.getProductVariants().enqueue(object : Callback<List<ProductVariant>> {
            override fun onResponse(call: Call<List<ProductVariant>>, response: Response<List<ProductVariant>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<ProductVariant>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun getProducts(onResult: (List<Product>?) -> Unit) {
        productApi.getProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun getCategories(onResult: (List<Category>?) -> Unit) {
        categoryApi.getCategories().enqueue(object : Callback<List<Category>> {
            override fun onResponse(call: Call<List<Category>>, response: Response<List<Category>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<Category>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun getCartItems(onResult: (List<CartItem>?) -> Unit) {
        cartApi.getCartItems().enqueue(object : Callback<List<CartItem>> {
            override fun onResponse(call: Call<List<CartItem>>, response: Response<List<CartItem>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<CartItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun getWishlistItems(onResult: (List<WishlistItem>?) -> Unit) {
        wishlistApi.getWishlistItems().enqueue(object : Callback<List<WishlistItem>> {
            override fun onResponse(call: Call<List<WishlistItem>>, response: Response<List<WishlistItem>>) {
                onResult(response.body())
            }
            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
