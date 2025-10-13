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
    private val productImageApi = ApiClient.retrofit.create(ProductImageApiService::class.java)

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

    fun getProductImages(onResult: (List<ProductImage>?) -> Unit) {
        productImageApi.getProductImages().enqueue(object : Callback<List<ProductImage>> {
            override fun onResponse(call: Call<List<ProductImage>>, response: Response<List<ProductImage>>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<ProductImage>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    // Debug helper: log element classes of a raw product response list (useful when BE returns mixed tuples)
    fun debugPrintProductResponseElements(elements: List<Any>?) {
        if (elements == null) return
        elements.forEachIndexed { idx, el ->
            android.util.Log.d("MainRepository", "element[$idx] class=${el::class.java.name}")
        }
    }

    // 🛍️ Lấy danh sách sản phẩm (có token)
    fun getProducts(token: String, onResult: (List<Product>?) -> Unit) {
        val bearerToken = "Bearer $token"
        productApi.getProducts(bearerToken)
            .enqueue(object : Callback<BaseResponse<List<com.pbl6.fitme.model.ProductResponse>>> {

                override fun onResponse(
                    call: Call<BaseResponse<List<com.pbl6.fitme.model.ProductResponse>>>,
                    response: Response<BaseResponse<List<com.pbl6.fitme.model.ProductResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val respList = response.body()?.result ?: emptyList()
                        // Map ProductResponse -> Product used by the app
                        val products = respList.map { pr ->
                            // Convert image string urls into ProductImage objects with placeholder ids
                            val images = pr.images.mapIndexed { idx, url ->
                                // We don't have image ids from BE's simplified response, use index-based negative ids
                                ProductImage(
                                    imageId = idx.toLong() * -1,
                                    createdAt = null,
                                    imageUrl = url,
                                    isMain = idx == 0,
                                    updatedAt = null,
                                    productId = pr.productId
                                )
                            }

                            val variants = pr.variants.map { vr ->
                                ProductVariant(
                                    variantId = vr.variantId,
                                    color = vr.color,
                                    size = vr.size,
                                    price = vr.price,
                                    stockQuantity = vr.stockQuantity,
                                    productId = pr.productId
                                )
                            }

                            Product(
                                productId = pr.productId,
                                productName = pr.productName,
                                description = pr.description,
                                categoryName = pr.categoryName,
                                brandName = pr.brandName,
                                isActive = pr.isActive,
                                images = images,
                                variants = variants,
                                reviews = emptyList()
                            )
                        }

                        onResult(products)
                    } else {
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<List<com.pbl6.fitme.model.ProductResponse>>>, t: Throwable) {
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
