package com.pbl6.fitme.product

import android.content.Context
import android.util.Log // Thêm import Log
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.model.AddCartRequest
import com.pbl6.fitme.model.AddWishlistRequest
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.WishlistRequest
import com.pbl6.fitme.repository.MainRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.UUID

class ProductDetailViewModel : BaseViewModel() {

    private val mainRepository = MainRepository()

    // LiveData to hold the product details
    val product = MutableLiveData<Product?>()

    // LiveData for one-time events like navigation or showing toasts
    val onAddToCartSuccess = MutableLiveData<Boolean>()
    val onBuyNowSuccess = MutableLiveData<Boolean>()

    fun fetchProductById(token: String, productId: String) {
        isLoading.postValue(true)
        Log.d("ProductDetailVM", "Fetching product details for ID: $productId")
        mainRepository.getProductById(token, productId) { result ->
            isLoading.postValue(false)
            if (result != null) {
                product.postValue(result)
                Log.d("ProductDetailVM", "Product fetched successfully: ${result.productName}")
            } else {
                Log.e("ProductDetailVM", "Failed to fetch product details.")
            }
        }
    }

    fun addToCart(context: Context, token: String, variantId: UUID, quantity: Int = 1) {
        val request = AddCartRequest(variantId, quantity)
        val cartId = com.pbl6.fitme.session.SessionManager.getInstance().getOrCreateCartId(context).toString()
        Log.d("ProductDetailVM", "Attempting to add to cart: CartID=$cartId, VariantID=$variantId")
        mainRepository.addToCart(context, token, cartId, request) { success ->
            if (success) {
                onAddToCartSuccess.postValue(true)
                Log.d("ProductDetailVM", "Added to cart successfully (Remote).")
            } else {
                try {
                    com.pbl6.fitme.session.SessionManager.getInstance().addLocalCartItem(context, variantId, quantity)
                    onAddToCartSuccess.postValue(true)
                    Log.w("ProductDetailVM", "Failed to add to cart remotely, falling back to local storage.")
                } catch (ex: Exception) {
                    Log.e("ProductDetailVM", "Thêm vào giỏ hàng thất bại (Local Fallback Error)", ex)
                }
            }
        }
    }

    var getAccessToken: ((Context) -> String?)? = null

    // SỬA: Logic ban đầu bị lỗi (luôn gọi createAndAddItemToWishlist)
    fun addToWishlist(token: String, userId: String?, productId: UUID) {
        if (userId.isNullOrBlank()) {
            Log.e("ProductDetailVM", "addToWishlist failed: userId is null or blank.")
            return
        }

        val productRequest = AddWishlistRequest(productId)
        val defaultWishlistName = "My Wishlist"
        Log.d("ProductDetailVM", "Starting addToWishlist for user: $userId, product: $productId")

        // 1. Kiểm tra/Lấy Wishlist ID
        mainRepository.fetchUserWishlistId(token, userId) { wishlistId ->
            if (wishlistId != null) {
                Log.d("ProductDetailVM", "Wishlist found: $wishlistId. Adding item...")
                addItemToExistingWishlist(token, wishlistId, productRequest)
            } else {
                Log.d("ProductDetailVM", "No Wishlist found. Creating new wishlist...")
                createAndAddItemToWishlist(token, userId, defaultWishlistName, productRequest)
            }
        }
    }

    /**
     * Helper: Tạo Wishlist mới, sau đó dùng ID trả về để thêm sản phẩm.
     */
    private fun createAndAddItemToWishlist(
        token: String,
        userId: String,
        name: String,
        productRequest: AddWishlistRequest
    ) {
        val wishlistReq = WishlistRequest(name)
        Log.d("ProductDetailVM", "Calling createWishlist with userId (profileId): $userId")

        // 1. GỌI API TẠO WISHLIST (Trả về ID)
        mainRepository.createWishlist(token, userId, wishlistReq) { newWishlistId ->
            if (newWishlistId != null) {
                Log.d("ProductDetailVM", "Wishlist created successfully with ID: $newWishlistId")
                // 2. TẠO THÀNH CÔNG VÀ CÓ ID -> Thêm sản phẩm
                addItemToExistingWishlist(token, newWishlistId, productRequest)
            } else {
                // Lỗi: Không thể tạo Wishlist mới (API tạo trả về null)
                Log.e("ProductDetailVM", "Failed to create new wishlist for user: $userId")
            }
        }
    }

    /**
     * Helper: Thực hiện bước cuối cùng là thêm sản phẩm vào Wishlist đã có ID.
     * Hàm này gọi hàm public trong MainRepository, giả định nó được public.
     */
    private fun addItemToExistingWishlist(
        token: String,
        wishlistId: String,
        productRequest: AddWishlistRequest
    ) {
        val bearerToken = "Bearer $token"
        // Gọi hàm helper trong Repository (phải đảm bảo hàm này có access modifier là public
        // trong MainRepository hoặc có một hàm public tương đương).
        Log.d("ProductDetailVM", "Adding product ${productRequest.productId} to existing wishlist $wishlistId")

        // **QUAN TRỌNG:** Trong MainRepository, hàm addItemToWishlist là private helper nhận bearer.
        // Ta cần gọi hàm public interface `addToWishlist` của repo để nó tự xử lý bearer:
        // **GIẢ ĐỊNH** bạn đã tạo một hàm public trong MainRepository (hoặc sửa hàm `addItemToWishlist` đã thấy):
        // fun addItemToWishlistPublic(token: String, wishlistId: String, req: AddWishlistRequest, onResult: (Boolean) -> Unit)

        // SỬ DỤNG HÀM `addItemToWishlist` (CẦN SỬA LẠI TRONG REPOSITORY ĐỂ CHẤP NHẬN TOKEN):

        // Dựa trên file MainRepository đã cung cấp, hàm addItemToWishlist nhận BEARER:
        // fun addItemToWishlist(bearer: String, wishlistId: String, req: AddWishlistRequest, onResult: (Boolean) -> Unit)

        // Ta sẽ gọi hàm đó và truyền Bearer thủ công.
        mainRepository.addItemToWishlist(bearerToken, wishlistId, productRequest) { success ->
            if (success) {
                Log.d("ProductDetailVM", "Product added to wishlist successfully.")
            } else {
            }
        }
    }
}