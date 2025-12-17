package com.pbl6.fitme.checkin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CheckInViewModel : ViewModel() {
    private val _diamond = MutableLiveData<Int>()
    val diamond: LiveData<Int> get() = _diamond

    fun setDiamond(value: Int) {
        _diamond.value = value
    }
}