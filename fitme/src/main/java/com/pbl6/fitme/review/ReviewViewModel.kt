package com.pbl6.fitme.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.ReviewRepository
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class ReviewViewModel : BaseViewModel() {

    private val repository = ReviewRepository()
    private val main = MainRepository()

    private val _createReviewResult = MutableLiveData<Boolean>()
    val createReviewResult: LiveData<Boolean> get() = _createReviewResult

    private val _isLoading = MutableLiveData<Boolean>()

    fun getProductIdByVariantId(
        token: String,
        variantId: String,
        onResult: (String?) -> Unit
    ) {
        main.getProductByVariantId(token, variantId) { product ->
            onResult(product?.productId.toString()) /// ⭐ Không .toString()
        }
    }

    fun submitReview(
        userEmail: String?,
        productId: String,
        rating: Int,
        comment: String
    ) {
        if (userEmail.isNullOrEmpty()) {
            _createReviewResult.value = false
            return
        }

        _isLoading.value = true
        repository.createReview(userEmail, productId, rating, comment) { review ->
            _isLoading.postValue(false)
            _createReviewResult.postValue(review != null)
        }
    }
}

