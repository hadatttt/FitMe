package com.pbl6.fitme.repository

import android.util.Log
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
import com.pbl6.fitme.model.MomoResponse
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
    private val userApi = ApiClient.retrofit.create(com.pbl6.fitme.network.UserApiService::class.java)
    private val variantApi = ApiClient.retrofit.create(ProductVariantApiService::class.java)
    private val productImageApi = ApiClient.retrofit.create(ProductImageApiService::class.java)
    private val reviewApi = ApiClient.retrofit.create(ReviewApiService::class.java)
    private val orderApi = ApiClient.retrofit.create(OrderApiService::class.java)
    private val momoApi = ApiClient.retrofit.create(com.pbl6.fitme.network.MomoApiService::class.java)

    fun createCartForNewUser(userId: String, onResult: (String?) -> Unit) {
        cartApi.createCartForUser(userId).enqueue(object : Callback<com.pbl6.fitme.network.ShoppingCartResponse> {
            override fun onResponse(call: Call<com.pbl6.fitme.network.ShoppingCartResponse>, response: Response<com.pbl6.fitme.network.ShoppingCartResponse>) {
                if (response.isSuccessful) {
                    val cartId = response.body()?.cartId
                    Log.d("MainRepository", "Shopping cart created for user: cartId=$cartId")
                    onResult(cartId)
                } else {
                    Log.e("MainRepository", "Failed to create cart: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<com.pbl6.fitme.network.ShoppingCartResponse>, t: Throwable) {
                Log.e("MainRepository", "createCartForNewUser network error", t)
                onResult(null)
            }
        })
    }
    fun getProductVariants(token: String, onResult: (List<ProductVariant>?) -> Unit) {
        val bearerToken = "Bearer $token"
        variantApi.getProductVariants(bearerToken).enqueue(object : Callback<BaseResponse<List<ProductVariant>>> {
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

    fun debugPrintProductResponseElements(elements: List<Any>?) {
        if (elements == null) return
        elements.forEachIndexed { idx, el ->
            android.util.Log.d("MainRepository", "element[$idx] class=${el::class.java.name}")
        }
    }

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
                                productName = pr.productName,
                                description = pr.description,
                                categoryName = pr.categoryName,
                                brandName = pr.brandName,
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

    // Fetch product by variant id (uses backend /products/by-variant/{variantId})
    fun getProductByVariantId(token: String, variantId: String, onResult: (Product?) -> Unit) {
        val bearer = "Bearer $token"
        productApi.getProductByVariant(bearer, variantId).enqueue(object : Callback<BaseResponse<com.pbl6.fitme.model.ProductResponse>> {
            override fun onResponse(call: Call<BaseResponse<com.pbl6.fitme.model.ProductResponse>>, response: Response<BaseResponse<com.pbl6.fitme.model.ProductResponse>>) {
                if (response.isSuccessful) {
                    val pr = response.body()?.result
                    if (pr == null) {
                        onResult(null)
                        return
                    }

                    val images = pr.images.mapIndexed { idx, url ->
                        com.pbl6.fitme.model.ProductImage(
                            imageId = idx.toLong() * -1,
                            createdAt = null,
                            imageUrl = url,
                            isMain = idx == 0,
                            updatedAt = null,
                            productId = pr.productId
                        )
                    }

                    val variants = pr.variants.map { vr ->
                        com.pbl6.fitme.model.ProductVariant(
                            variantId = vr.variantId,
                            color = vr.color,
                            size = vr.size,
                            price = vr.price,
                            stockQuantity = vr.stockQuantity,
                            productId = pr.productId
                        )
                    }

                    val product = com.pbl6.fitme.model.Product(
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

            override fun onFailure(call: Call<BaseResponse<com.pbl6.fitme.model.ProductResponse>>, t: Throwable) {
                android.util.Log.e("MainRepository", "getProductByVariantId network error", t)
                onResult(null)
            }
        })
    }

    // Fetch user profile by email and save userId (profileId) into SessionManager
    // Note: UserResponse.userId is actually the profileId (not accountId), which is needed for wishlist operations
    fun fetchAndStoreUserId(token: String?, email: String?, onResult: (String?) -> Unit) {
        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            android.util.Log.w("MainRepository", "fetchAndStoreUserId: token or email is blank")
            onResult(null)
            return
        }
        android.util.Log.d("MainRepository", "fetchAndStoreUserId: calling GET users/user-profile/$email")
        val bearer = "Bearer $token"
        userApi.getUserProfileByEmail(bearer, email!!).enqueue(object : retrofit2.Callback<BaseResponse<com.pbl6.fitme.model.UserResponse>> {
            override fun onResponse(call: retrofit2.Call<BaseResponse<com.pbl6.fitme.model.UserResponse>>, response: retrofit2.Response<BaseResponse<com.pbl6.fitme.model.UserResponse>>) {
                if (response.isSuccessful) {
                    try {
                        val user = response.body()?.result
                        val profileId = user?.userId  // UserResponse.userId is actually profileId from backend
                        android.util.Log.d("MainRepository", "fetchAndStoreUserId response SUCCESS")
                        android.util.Log.d("MainRepository", "  - user?.userId=$profileId (this is profileId)")
                        android.util.Log.d("MainRepository", "  - user?.email=${user?.email}")
                        android.util.Log.d("MainRepository", "  - user?.fullName=${user?.fullName}")
                        android.util.Log.d("MainRepository", "  - full user object=$user")
                        onResult(profileId)
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "fetchAndStoreUserId parse failed", ex)
                        onResult(null)
                    }
                } else {
                    try {
                        val body = response.errorBody()?.string()
                        android.util.Log.e("MainRepository", "fetchAndStoreUserId failed code=${response.code()} body=$body")
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "fetchAndStoreUserId failed code=${response.code()}")
                    }
                    onResult(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<BaseResponse<com.pbl6.fitme.model.UserResponse>>, t: Throwable) {
                android.util.Log.e("MainRepository", "fetchAndStoreUserId network failure", t)
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
    fun getProductsByCategory(token: String, categoryId: String, onResult: (List<Product>?) -> Unit) {
        val bearerToken = "Bearer $token"
        productApi.getProductsByCategory(bearerToken, categoryId)
            .enqueue(object : Callback<BaseResponse<List<ProductResponse>>> {

                override fun onResponse(
                    call: Call<BaseResponse<List<ProductResponse>>>,
                    response: Response<BaseResponse<List<ProductResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val respList = response.body()?.result ?: emptyList()
                        val products = respList.map { pr ->
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
                                productName = pr.productName,
                                description = pr.description,
                                categoryName = pr.categoryName,
                                brandName = pr.brandName,
                                isActive = pr.isActive,
                                images = images,
                                variants = variants
                            )
                        }

                        android.util.Log.d("MainRepository", "getProductsByCategory: Found ${products.size} items for catId=$categoryId")
                        onResult(products)
                    } else {
                        android.util.Log.e("MainRepository", "getProductsByCategory failed: ${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(
                    call: Call<BaseResponse<List<ProductResponse>>>,
                    t: Throwable
                ) {
                    android.util.Log.e("MainRepository", "getProductsByCategory network error", t)
                    onResult(null)
                }
            })
    }

    fun getCartItems(token: String, cartId: String, onResult: (List<CartItem>?) -> Unit) {
        val bearerToken = "Bearer $token"
        cartApi.getCartItems(bearerToken, cartId).enqueue(object : Callback<List<CartItem>> {
            override fun onResponse(call: Call<List<CartItem>>, response: Response<List<CartItem>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MainRepository", "getCartItems failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<CartItem>>, t: Throwable) {
                Log.e("MainRepository", "getCartItems network error", t)
                onResult(null)
            }
        })
    }

    /**
     * Fetch server-side shopping cart for the given profile/user ID. Returns the cartId string
     * via callback or null if not found or on error.
     */
    fun getCartByUser(token: String?, profileId: String?, onResult: (String?) -> Unit) {
        if (token.isNullOrBlank() || profileId.isNullOrBlank()) {
            onResult(null)
            return
        }
        val bearer = "Bearer $token"
        cartApi.getCartByUser(bearer, profileId).enqueue(object : Callback<com.pbl6.fitme.network.ShoppingCartResponse> {
            override fun onResponse(call: Call<com.pbl6.fitme.network.ShoppingCartResponse>, response: Response<com.pbl6.fitme.network.ShoppingCartResponse>) {
                if (response.isSuccessful) {
                    val cartId = response.body()?.cartId
                    onResult(cartId)
                } else {
                    android.util.Log.e("MainRepository", "getCartByUser failed: ${response.code()} body=${response.errorBody()?.string()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<com.pbl6.fitme.network.ShoppingCartResponse>, t: Throwable) {
                android.util.Log.e("MainRepository", "getCartByUser network error", t)
                onResult(null)
            }
        })
    }
    fun getWishlistItems(onResult: (List<WishlistItem>?) -> Unit) {
        // This method now requires a wishlistId. Keep a stub implementation to avoid breaking callers.
        Log.e("MainRepository", "getWishlistItems called without wishlistId - not supported")
        onResult(null)
    }

    // Add variant to cart (uses session cartId)
    fun addToCart(context: android.content.Context, token: String, cartId: String, req: com.pbl6.fitme.model.AddCartRequest, onResult: (Boolean) -> Unit) {
        android.util.Log.d("MainRepository", "Adding to cart: cartId=$cartId variantId=${req.variantId} quantity=${req.quantity}")
        val bearerToken = "Bearer $token"
        cartApi.addOrUpdateCartItem(bearerToken, cartId, req).enqueue(object : Callback<com.pbl6.fitme.model.CartItem> {
            override fun onResponse(call: Call<com.pbl6.fitme.model.CartItem>, response: Response<com.pbl6.fitme.model.CartItem>) {
                if (response.isSuccessful) {
                    android.util.Log.d("MainRepository", "Successfully added to cart")
                    onResult(true)
                } else {
                    try {
                        val body = response.errorBody()?.string()
                        android.util.Log.e("MainRepository", "addToCart failed code=${response.code()} body=$body")
                        android.util.Log.e("MainRepository", "Request URL: ${call.request().url}")
                    } catch (ex: Exception) {
                        android.util.Log.e("MainRepository", "addToCart failed code=${response.code()}")
                    }
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<com.pbl6.fitme.model.CartItem>, t: Throwable) {
                android.util.Log.e("MainRepository", "addToCart network failure", t)
                onResult(false)
            }
        })
    }

    // Add product to wishlist (ensures header uses profileId)
    fun addToWishlist(
        token: String,
        userEmail: String?, // user email used by backend
        req: AddWishlistRequest,
        onResult: (Boolean) -> Unit
    ) {
        val bearer = "Bearer $token"

        if (userEmail.isNullOrBlank()) {
            Log.e("MainRepository", "Cannot add to wishlist: userEmail is null or blank")
            onResult(false)
            return
        }

        Log.d("MainRepository", "addToWishlist: using userEmail=$userEmail")

        // 1) Fetch existing wishlists for the user
        wishlistApi.getWishlistsByUser(bearer, userEmail).enqueue(object : Callback<List<com.pbl6.fitme.model.WishlistDto>> {
            override fun onResponse(
                call: Call<List<com.pbl6.fitme.model.WishlistDto>>,
                response: Response<List<com.pbl6.fitme.model.WishlistDto>>
            ) {
                if (response.isSuccessful) {
                    val wishlists = response.body() ?: emptyList()
                    if (wishlists.isNotEmpty()) {
                        // Use first wishlist
                        val wishlistId = wishlists[0].wishlistId.toString()
                        addItemToWishlist(bearer, wishlistId, req, onResult)
                    } else {
                        // No wishlist: create one first
                        val wishlistReq = com.pbl6.fitme.model.WishlistRequest(name = "My Wishlist")
                        wishlistApi.createWishlist(userEmail, bearer, wishlistReq)
                            .enqueue(object : Callback<com.pbl6.fitme.network.WishlistResponse> {
                                override fun onResponse(
                                    call: Call<com.pbl6.fitme.network.WishlistResponse>,
                                    response: Response<com.pbl6.fitme.network.WishlistResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        val createdWishlistId = response.body()?.wishlistId
                                        if (!createdWishlistId.isNullOrBlank()) {
                                            addItemToWishlist(bearer, createdWishlistId, req, onResult)
                                        } else {
                                            Log.e("MainRepository", "createWishlist returned empty wishlistId")
                                            onResult(false)
                                        }
                                    } else {
                                        Log.e("MainRepository", "createWishlist failed code=${response.code()} body=${response.errorBody()?.string()}")
                                        onResult(false)
                                    }
                                }

                                

                                override fun onFailure(call: Call<com.pbl6.fitme.network.WishlistResponse>, t: Throwable) {
                                    Log.e("MainRepository", "createWishlist network failure", t)
                                    onResult(false)
                                }
                            })
                    }
                } else {
                    Log.e("MainRepository", "getWishlistsByUser failed code=${response.code()} body=${response.errorBody()?.string()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<List<com.pbl6.fitme.model.WishlistDto>>, t: Throwable) {
                Log.e("MainRepository", "getWishlistsByUser network failure", t)
                onResult(false)
            }
        })
    }

    /**
     * Create a new wishlist for the given profile (user) and return the created wishlistId as String
     * via the callback, or null on error.
     */
    fun createWishlist(token: String?, userEmail: String?, req: com.pbl6.fitme.model.WishlistRequest, onResult: (String?) -> Unit) {
        if (token.isNullOrBlank() || userEmail.isNullOrBlank()) {
            onResult(null)
            return
        }
        val bearer = "Bearer $token"
        wishlistApi.createWishlist(userEmail, bearer, req).enqueue(object : Callback<com.pbl6.fitme.network.WishlistResponse> {
            override fun onResponse(call: Call<com.pbl6.fitme.network.WishlistResponse>, response: Response<com.pbl6.fitme.network.WishlistResponse>) {
                if (response.isSuccessful) {
                    val createdId = response.body()?.wishlistId
                    onResult(createdId)
                } else {
                    Log.e("MainRepository", "createWishlist failed: ${response.code()} body=${response.errorBody()?.string()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<com.pbl6.fitme.network.WishlistResponse>, t: Throwable) {
                Log.e("MainRepository", "createWishlist network failure", t)
                onResult(null)
            }
        })
    }

    /**
     * Fetch the first wishlist ID for the given user/profile. Returns the wishlistId as String
     * or null if none exists or on error.
     */
    fun fetchUserWishlistId(token: String?, userEmail: String?, onResult: (String?) -> Unit) {
        if (token.isNullOrBlank() || userEmail.isNullOrBlank()) {
            onResult(null)
            return
        }
        val bearer = "Bearer $token"
        wishlistApi.getWishlistsByUser(bearer, userEmail).enqueue(object : Callback<List<com.pbl6.fitme.model.WishlistDto>> {
            override fun onResponse(call: Call<List<com.pbl6.fitme.model.WishlistDto>>, response: Response<List<com.pbl6.fitme.model.WishlistDto>>) {
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    if (list.isNotEmpty()) {
                        onResult(list[0].wishlistId.toString())
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<com.pbl6.fitme.model.WishlistDto>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    // Helper to add item to wishlist by wishlistId
    // made public so callers (ViewModels) can add items given a bearer or via public wrapper
    fun addItemToWishlist(
        bearer: String,
        wishlistId: String,
        req: AddWishlistRequest,
        onResult: (Boolean) -> Unit
    ) {
        wishlistApi.addItemToWishlist(bearer, wishlistId, req).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
                if (!response.isSuccessful) {
                    Log.e("MainRepository", "addItemToWishlist failed code=${response.code()} body=${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MainRepository", "addItemToWishlist network failure", t)
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
                    val body = response.body()
                    // Backend may return the payload under `result` or `data`.
                    val payload = body?.result ?: body?.data
                    onResult(payload)
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

    // Create Momo payment (calls backend /momopay/create)
    fun createMomoPayment(token: String, amount: Long, userEmail: String, orderId: String?, onResult: (MomoResponse?) -> Unit) {
        val bearer = "Bearer $token"
        momoApi.createMomoPayment(bearer, amount, userEmail, orderId).enqueue(object : Callback<MomoResponse> {
            override fun onResponse(call: Call<MomoResponse>, response: Response<MomoResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    android.util.Log.e("MainRepository", "createMomoPayment failed: ${response.code()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<MomoResponse>, t: Throwable) {
                android.util.Log.e("MainRepository", "createMomoPayment network error", t)
                onResult(null)
            }
        })
    }

    // Handle VNPay callback query params by calling backend endpoint
    fun handleVNPayCallback(
        vnpResponseCode: String,
        vnpOrderInfo: String,
        vnpTransactionStatus: String,
        vnpTransactionNo: String?,
        vnpPayDate: String?,
        onResult: (VNPayResponse?) -> Unit
    ) {
        orderApi.handleVNPayCallback(vnpResponseCode, vnpOrderInfo, vnpTransactionStatus, vnpTransactionNo, vnpPayDate)
            .enqueue(object : Callback<BaseResponse<VNPayResponse>> {
                override fun onResponse(call: Call<BaseResponse<VNPayResponse>>, response: Response<BaseResponse<VNPayResponse>>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = body?.result ?: body?.data
                        onResult(payload)
                    } else {
                        android.util.Log.e("MainRepository", "handleVNPayCallback failed: ${response.code()}")
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<VNPayResponse>>, t: Throwable) {
                    android.util.Log.e("MainRepository", "handleVNPayCallback network error", t)
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