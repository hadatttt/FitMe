package com.pbl6.fitme.product

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.model.AddCartRequest
import com.pbl6.fitme.model.AddWishlistRequest
import com.pbl6.fitme.model.Product
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
    val onAddToWishlistSuccess = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    fun fetchProductById(token: String, productId: String) {
        isLoading.postValue(true) // Show loading indicator from BaseViewModel
        mainRepository.getProductById(token, productId) { result ->
            isLoading.postValue(false) // Hide loading indicator
            if (result != null) {
                product.postValue(result)
            } else {
                errorMessage.postValue("Không lấy được thông tin sản phẩm")
            }
        }
    }

    fun addToCart(token: String, variantId: UUID, quantity: Int = 1) {
        val request = AddCartRequest(variantId, quantity)
        mainRepository.addToCart(token, request) { success ->
            if (success) {
                onAddToCartSuccess.postValue(true)
            } else {
                errorMessage.postValue("Thêm vào giỏ hàng thất bại")
            }
        }
    }


    fun buyNow(token: String, variantId: UUID, quantity: Int = 1) {
        val request = AddCartRequest(variantId, quantity)
        mainRepository.addToCart(token, request) { success ->
            if (success) {
                onBuyNowSuccess.postValue(true)
            } else {
                errorMessage.postValue("Không thể đặt hàng")
            }
        }
    }
    var getAccessToken: ((Context) -> String?)? = null
    fun addToWishlist(token: String, productId: UUID) {
        val request = AddWishlistRequest(productId)
        mainRepository.addToWishlist(token, request) { success ->
            if (success) {
                onAddToWishlistSuccess.postValue(true)
            } else {
                errorMessage.postValue("Thêm vào wishlist thất bại")
            }
        }
    }
}