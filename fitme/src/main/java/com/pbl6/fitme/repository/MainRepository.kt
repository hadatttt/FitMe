package com.pbl6.fitme.repository

import Category
import com.pbl6.fitme.model.*
import com.pbl6.fitme.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainRepository {

    private val productApi = ApiClient.retrofit.create(ProductApiService::class.java)
    private val categoryApi = ApiClient.retrofit.create(CategoryApiService::class.java)
    private val cartApi = ApiClient.retrofit.create(CartApiService::class.java)
    private val wishlistApi = ApiClient.retrofit.create(WishlistApiService::class.java)
    private val variantApi = ApiClient.retrofit.create(ProductVariantApiService::class.java)

    // 🧩 Lấy danh sách biến thể sản phẩm
    fun getProductVariants(onResult: (List<ProductVariant>?) -> Unit) {
        variantApi.getProductVariants().enqueue(object : Callback<List<ProductVariant>> {
            override fun onResponse(
                call: Call<List<ProductVariant>>,
                response: Response<List<ProductVariant>>
            ) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<ProductVariant>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    // 🛍️ Lấy danh sách sản phẩm (có token)
    fun getProducts(token: String, onResult: (List<Product>?) -> Unit) {
        val bearerToken = "Bearer $token"

        // Callback phải khớp với kiểu trả về của ApiService
        productApi.getProducts(bearerToken).enqueue(object : Callback<BaseResponse<List<Product>>> { // <-- Sửa ở đây

            override fun onResponse(
                call: Call<BaseResponse<List<Product>>>,
                response: Response<BaseResponse<List<Product>>> // <-- Sửa cả ở đây
            ) {
                if (response.isSuccessful) {
                    // Lấy dữ liệu từ trường 'result' bên trong BaseResponse
                    onResult(response.body()?.result) // <-- Thay đổi quan trọng nhất!
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<List<Product>>>, t: Throwable) {
                onResult(null)
            }
        })
    }
    fun getCategories(token: String, onResult: (List<Category>?) -> Unit) {
        val bearerToken = "Bearer $token"
        categoryApi.getCategories(bearerToken)
            .enqueue(object : Callback<BaseResponse<List<Category>>> {
                override fun onResponse(
                    call: Call<BaseResponse<List<Category>>>,
                    response: Response<BaseResponse<List<Category>>>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body()?.result)
                    } else {
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<List<Category>>>, t: Throwable) {
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
            override fun onResponse(
                call: Call<List<WishlistItem>>,
                response: Response<List<WishlistItem>>
            ) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
