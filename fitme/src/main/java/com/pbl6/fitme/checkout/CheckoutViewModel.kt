package com.pbl6.fitme.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import java.util.UUID

class CheckoutViewModel : BaseViewModel() {
    
    private val _currentOrderId = MutableLiveData<UUID?>(null)
    val currentOrderId: LiveData<UUID?> = _currentOrderId

    fun getCurrentOrderId(): UUID? {
        return _currentOrderId.value
    }

    fun setCurrentOrderId(orderId: UUID?) {
        _currentOrderId.value = orderId
    }

    fun clearCurrentOrderId() {
        _currentOrderId.value = null
    }
}