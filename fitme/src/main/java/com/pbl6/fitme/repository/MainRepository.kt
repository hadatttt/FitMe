package com.pbl6.fitme.repository

import android.util.Log
import com.pbl6.fitme.model.AddCartRequest
import com.pbl6.fitme.model.AddWishlistRequest
import com.pbl6.fitme.network.BaseResponse
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.model.Order
import com.pbl6.fitme.model.OrderStatus
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductImage
import com.pbl6.fitme.model.ProductResponse
import com.pbl6.fitme.model.ProductVariant
import com.pbl6.fitme.model.VNPayResponse
import com.pbl6.fitme.model.WishlistItem
import com.pbl6.fitme.network.ApiClient
import com.pbl6.fitme.network.CartApiService
import com.pbl6.fitme.network.CategoryApiService
import com.pbl6.fitme.network.OrderApiService
import com.pbl6.fitme.network.ProductApiService
import com.pbl6.fitme.network.ProductImageApiService
import com.pbl6.fitme.network.ProductVariantApiService
import com.pbl6.fitme.network.ReviewApiService
import com.pbl6.fitme.network.WishlistApiService
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
    private val reviewApi = ApiClient.retrofit.create(ReviewApiService::class.java)
    private val orderApi = ApiClient.retrofit.create(OrderApiService::class.java)

    // 🧩 Lấy danh sách biến thể sản phẩm
    fun getProductVariants(onResult: (List<ProductVariant>?) -> Unit) {
        variantApi.getProductVariants().enqueue(object : Callback<BaseResponse<List<ProductVariant>>> {
            override fun onResponse(call: Call<BaseResponse<List<ProductVariant>>>, response: Response<BaseResponse<List<ProductVariant>>>) {
                if (response.isSuccessful) {
                    onResult(response.body()?.result)
                } else {
                    Log.e("MainRepository", "getProductVariants failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<List<ProductVariant>>>, t: Throwable) {
                Log.e("MainRepository", "getProductVariants network error", t)
                onResult(null)
            }
        })
    }

    fun getProductImages(onResult: (List<ProductImage>?) -> Unit) {
        productImageApi.getProductImages().enqueue(object : Callback<List<ProductImage>> {
            override fun onResponse(call: Call<List<ProductImage>>, response: Response<List<ProductImage>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MainRepository", "getProductImages failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<ProductImage>>, t: Throwable) {
                Log.e("MainRepository", "getProductImages network error", t)
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
            .enqueue(object : Callback<BaseResponse<List<ProductResponse>>> {

                override fun onResponse(
                    call: Call<BaseResponse<List<ProductResponse>>>,
                    response: Response<BaseResponse<List<ProductResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val respList = response.body()?.result ?: emptyList()
                        // Map ProductResponse -> Product used by the app
                        val products = respList.map { pr ->
                            // Convert image string urls into ProductImage objects with placeholder ids
                            val images = pr.images.mapIndexed { idx, url ->
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
                                createdAt = pr.createdAt,
                                productName = pr.productName,
                                description = pr.description,
                                categoryName = pr.categoryName,
                                brandName = pr.brandName,
                                gender = pr.gender,
                                season = pr.season,
                                isActive = pr.isActive,
                                images = images,
                                variants = variants
                            )
                        }

                        onResult(products)
                    } else {
                        onResult(null)
                    }
                }


                override fun onFailure(
                    call: Call<BaseResponse<List<ProductResponse>>>,
                    t: Throwable
                ) {
                    Log.e("MainRepository", "getProducts network error", t)
                    onResult(null)
                }
            })
    }

    // Lấy chi tiết 1 product theo id
    fun getProductById(token: String, id: String, onResult: (Product?) -> Unit) {
        val bearer = "Bearer $token"
        productApi.getProductById(bearer, id).enqueue(object : Callback<BaseResponse<ProductResponse>> {
            override fun onResponse(
                call: Call<BaseResponse<ProductResponse>>,
                response: Response<BaseResponse<ProductResponse>>
            ) {
                if (response.isSuccessful) {
                    val pr = response.body()?.result
                    if (pr == null) {
                        onResult(null)
                        return
                    }

                    val images = pr.images.mapIndexed { idx, url ->
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

                    val product = Product(
                        productId = pr.productId,
                        createdAt = pr.createdAt,
                        productName = pr.productName,
                        description = pr.description,
                        categoryName = pr.categoryName,
                        brandName = pr.brandName,
                        gender = pr.gender,
                        season = pr.season,
                        isActive = pr.isActive,
                        images = images,
                        variants = variants
                    )

                    onResult(product)
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<ProductResponse>>, t: Throwable) {
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
        cartApi.getCartItems().enqueue(object : Callback<BaseResponse<List<CartItem>>> {
            override fun onResponse(call: Call<BaseResponse<List<CartItem>>>, response: Response<BaseResponse<List<CartItem>>>) {
                if (response.isSuccessful) {
                    onResult(response.body()?.result)
                } else {
                    Log.e("MainRepository", "getCartItems failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<List<CartItem>>>, t: Throwable) {
                Log.e("MainRepository", "getCartItems network error", t)
                onResult(null)
            }
        })
    }
    fun getWishlistItems(onResult: (List<WishlistItem>?) -> Unit) {
        wishlistApi.getWishlistItems().enqueue(object : Callback<List<WishlistItem>> {
            override fun onResponse(call: Call<List<WishlistItem>>, response: Response<List<WishlistItem>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MainRepository", "getWishlistItems failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<WishlistItem>>, t: Throwable) {
                Log.e("MainRepository", "getWishlistItems network error", t)
                onResult(null)
            }
        })
    }

    // Add variant to cart
    fun addToCart(token: String, req: com.pbl6.fitme.model.AddCartRequest, onResult: (Boolean) -> Unit) {
        val bearer = "Bearer $token"
        android.util.Log.d("MainRepository", "Adding to cart: variantId=${req.variantId} quantity=${req.quantity}")
        cartApi.addToCart(bearer, req).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    android.util.Log.d("MainRepository", "Successfully added to cart")
                    onResult(true)
                } else {
                    try {
                        val body = response.errorBody()?.string()
                        android.util.Log.e("MainRepository", "addToCart failed code=${response.code()} body=$body")
                        android.util.Log.e("MainRepository", "Request URL: ${call.request().url}")
                        android.util.Log.e("MainRepository", "Request headers: ${call.request().headers}")
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "addToCart failed code=${response.code()} (error reading body)")
                    }
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                android.util.Log.e("MainRepository", "addToCart network failure", t)
                onResult(false)
            }
        })
    }

    // Add product to wishlist
    fun addToWishlist(token: String, req: com.pbl6.fitme.model.AddWishlistRequest, onResult: (Boolean) -> Unit) {
        val bearer = "Bearer $token"
        wishlistApi.addToWishlist(bearer, req).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    try {
                        val body = response.errorBody()?.string()
                        android.util.Log.e("MainRepository", "addToWishlist failed code=${response.code()} body=$body")
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "addToWishlist failed code=${response.code()} (error reading body)")
                    }
                    onResult(false)
                }
            }



            override fun onFailure(call: Call<Void>, t: Throwable) {
                android.util.Log.e("MainRepository", "addToWishlist network failure", t)
                onResult(false)
            }
        })
    }

    // Create an order on the backend
    fun createOrder(token: String, order: Order, onResult: (Order?) -> Unit) {
        val bearer = "Bearer $token"
        android.util.Log.d("MainRepository", "Creating order: items=${order.orderItems.map { "${it.variantId}:${it.quantity}" }}")
        android.util.Log.d("MainRepository", "Order details: userEmail=${order.userEmail}, shipping=${order.shippingAddress}")

        orderApi.createOrder(bearer, order).enqueue(object : Callback<BaseResponse<Order>> {
            override fun onResponse(call: Call<BaseResponse<Order>>, response: Response<BaseResponse<Order>>) {
                if (response.isSuccessful) {
                    android.util.Log.d("MainRepository", "Order created successfully")
                    onResult(response.body()?.result)
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("MainRepository", "createOrder failed: ${response.code()}")
                        android.util.Log.e("MainRepository", "Error response: $errorBody")
                        android.util.Log.e("MainRepository", "Request URL: ${call.request().url}")
                        android.util.Log.e("MainRepository", "Request: email=${order.userEmail}")
                        android.util.Log.e("MainRepository", "Request: items=${order.orderItems.size}")
                        android.util.Log.e("MainRepository", "Request: total=${order.totalAmount}")
                        android.util.Log.e("MainRepository", "Request: shipping=${order.shippingAddress}")
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "Error reading error response", ex)
                    }
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<Order>>, t: Throwable) {
                android.util.Log.e("MainRepository", "createOrder network error", t)
                onResult(null)
            }
        })
    }

    // Create VNPay payment URL for the given order
    fun createVNPayPayment(token: String, orderId: String, userEmail: String, onResult: (VNPayResponse?) -> Unit) {
        val bearer = "Bearer $token"
        orderApi.createVNPayPayment(bearer, orderId, userEmail).enqueue(object : Callback<BaseResponse<VNPayResponse>> {
            override fun onResponse(call: Call<BaseResponse<VNPayResponse>>, response: Response<BaseResponse<VNPayResponse>>) {
                if (response.isSuccessful) {
                    onResult(response.body()?.result)
                } else {
                    android.util.Log.e("MainRepository", "createVNPayPayment failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<VNPayResponse>>, t: Throwable) {
                android.util.Log.e("MainRepository", "createVNPayPayment network error", t)
                onResult(null)
            }
        })
    }

    // Get order details by ID
    fun getOrderById(token: String, orderId: String, onResult: (Order?) -> Unit) {
        val bearer = "Bearer $token"
        orderApi.getOrderById(bearer, orderId).enqueue(object : Callback<BaseResponse<Order>> {
            override fun onResponse(call: Call<BaseResponse<Order>>, response: Response<BaseResponse<Order>>) {
                if (response.isSuccessful) {
                    onResult(response.body()?.result)
                } else {
                    android.util.Log.e("MainRepository", "getOrderById failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<BaseResponse<Order>>, t: Throwable) {
                android.util.Log.e("MainRepository", "getOrderById network error", t)
                onResult(null)
            }
            })
        }

        // Get orders by user email and optional status 
        fun getOrdersByUser(token: String, email: String, status: OrderStatus?, onResult: (List<Order>?) -> Unit) {
            // Log parameters for debugging server 400 responses
            android.util.Log.d("MainRepository", "getOrdersByUser called. email='$email' status='${status?.name}' tokenPresent=${!token.isNullOrBlank()}")

            val bearer = "Bearer $token"
            // Send enum name (UPPERCASE) so backend receives PENDING, PROCESSING, etc.
            orderApi.getOrdersByUser(bearer, email, status?.name).enqueue(object : Callback<BaseResponse<List<Order>>> {
                override fun onResponse(call: Call<BaseResponse<List<Order>>>, response: Response<BaseResponse<List<Order>>>) {
                    if (response.isSuccessful) {
                        onResult(response.body()?.result)
                    } else {
                        android.util.Log.e("MainRepository", "getOrdersByUser failed: ${response.code()} - body=${response.errorBody()?.string()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<List<Order>>>, t: Throwable) {
                    android.util.Log.e("MainRepository", "getOrdersByUser network error", t)
                    onResult(null)
                }
            })
        }

        // Update order status
        fun updateOrderStatus(token: String, orderId: String, newStatus: OrderStatus, onResult: (Boolean) -> Unit) {
            val bearer = "Bearer $token"
            orderApi.updateOrderStatus(bearer, orderId, newStatus.name).enqueue(object : Callback<BaseResponse<Order>> {
                override fun onResponse(call: Call<BaseResponse<Order>>, response: Response<BaseResponse<Order>>) {
                    if (response.isSuccessful) {
                        android.util.Log.d("MainRepository", "Order status updated successfully")
                        onResult(true)
                    } else {
                        android.util.Log.e("MainRepository", "updateOrderStatus failed: ${response.code()}")
                        onResult(false)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Order>>, t: Throwable) {
                    android.util.Log.e("MainRepository", "updateOrderStatus network error", t)
                    onResult(false)
                }
            })
        }
}