package com.pbl6.fitme.product

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.model.AddWishlistRequest
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.Review
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.ReviewRepository
import com.pbl6.fitme.repository.WishlistRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.UUID

class ProductDetailViewModel : BaseViewModel() {

    private val mainRepository = MainRepository()
    private val reviewRepo = ReviewRepository()
    private val wishlistRepo = WishlistRepository()

    // --- DATA HOLDERS ---
    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val _reviews = MutableLiveData<List<Review>?>()
    val reviews: LiveData<List<Review>?> = _reviews

    private val _relatedProducts = MutableLiveData<List<Product>?>()
    val relatedProducts: LiveData<List<Product>?> = _relatedProducts

    // Wishlist State
    private val _isFavorite = MutableLiveData<Boolean>(false)
    val isFavorite: LiveData<Boolean> = _isFavorite
    private var currentWishlistItemId: String? = null // Stored internally for deletion

    // Events
    val onAddToCartSuccess = MutableLiveData<Boolean>()
    val onBuyNowSuccess = MutableLiveData<Boolean>()
    val toastMessage = MutableLiveData<String>() // Used by Fragment to show Toasts

    // Tracking variable for current ID to help with caching
    private var loadedProductId: String? = null

    /**
     * Main function to load data.
     * Logic: Only calls API if productId changes or data is missing.
     */
    fun loadData(token: String, productId: String, userEmail: String?) {
        // CACHING CHECK: If ID is the same and product data exists -> Do nothing (use cache)
        if (loadedProductId == productId && _product.value != null) {
            Log.d("ProductDetailVM", "Data already exists for ID: $productId. Using cache.")
            return
        }

        // If new ID or no data -> Reset and call APIs
        loadedProductId = productId
        _product.value = null
        _reviews.value = null
        _isFavorite.value = false
        currentWishlistItemId = null

        // Call APIs in parallel
        fetchProductById(token, productId)
        fetchProductReviews(token, productId)
        fetchRelatedProducts(token)

        if (!userEmail.isNullOrBlank()) {
            checkWishlistStatus(token, productId)
        }
    }

    private fun fetchProductById(token: String, productId: String) {
        isLoading.postValue(true)
        mainRepository.getProductById(token, productId) { result ->
            isLoading.postValue(false)
            if (result != null) {
                _product.postValue(result)
            } else {
                Log.e("ProductDetailVM", "Failed to fetch product details.")
            }
        }
    }

    private fun fetchProductReviews(token: String, productId: String) {
        reviewRepo.getReviewsByProduct(token, productId) { result ->
            // Update UI regardless of null/empty to hide/show review layout
            _reviews.postValue(result ?: emptyList())
        }
    }

    private fun fetchRelatedProducts(token: String) {
        // Only fetch if data is missing (related products usually change less frequently)
        if (_relatedProducts.value == null) {
            mainRepository.getProducts(token) { products ->
                if (products != null) {
                    _relatedProducts.postValue(products)
                }
            }
        }
    }

    // --- WISHLIST LOGIC ---

    private fun checkWishlistStatus(token: String, productId: String) {
        wishlistRepo.getWishlist(token) { items ->
            val found = items?.firstOrNull { wi ->
                try { wi.productId?.toString() == productId } catch (_: Exception) { false }
            }
            currentWishlistItemId = found?.wishlistItemId?.toString()
            _isFavorite.postValue(found != null)
        }
    }

    fun toggleWishlist(token: String, userEmail: String?, productId: String) {
        if (userEmail.isNullOrBlank()) {
            toastMessage.postValue("Please login to use Wishlist")
            return
        }

        val isCurrentlyFavorite = _isFavorite.value == true

        if (isCurrentlyFavorite) {
            // REMOVE
            val idToRemove = currentWishlistItemId
            if (idToRemove != null) {
                wishlistRepo.removeWishlistItem(token, idToRemove) { success ->
                    if (success) {
                        _isFavorite.postValue(false)
                        currentWishlistItemId = null
                        toastMessage.postValue("Removed from Wishlist")
                    } else {
                        toastMessage.postValue("Removal failed")
                    }
                }
            } else {
                // Rare case: isFavorite is true but ID is missing -> Refresh status
                checkWishlistStatus(token, productId)
            }
        } else {
            // ADD
            val request = AddWishlistRequest(UUID.fromString(productId))

            mainRepository.addToWishlist(token, userEmail, request) { success ->
                if (success) {
                    // Refresh to get the newly created ID
                    checkWishlistStatus(token, productId)
                    toastMessage.postValue("Added to Wishlist")
                } else {
                    toastMessage.postValue("Failed to add to Wishlist")
                }
            }
        }
    }
}