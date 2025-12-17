package com.pbl6.fitme.slotmachinegame

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class SlotMachineGameViewModel: BaseViewModel() {
    private val _spinsLeft= MutableLiveData<Int>()
    val spinsLeft: LiveData<Int> get() = _spinsLeft

    private val _diamondCount = MutableLiveData<Int>()
    val diamondCount: LiveData<Int> get() = _diamondCount

    fun setSpinsLeft(value: Int){
        _spinsLeft.value = value
    }
    fun decrementSpinsLeft() {
        val currentSpins = _spinsLeft.value ?: 0
        if (currentSpins > 0) {
            _spinsLeft.value = currentSpins - 1
        }
    }
    fun increaseSpinsLeft(amount: Int = 1) {
        val currentSpins = _spinsLeft.value ?: 0
        _spinsLeft.value = currentSpins + amount
    }

    fun setDiamondCount(value: Int) {
        _diamondCount.value = value
    }

    fun incrementDiamondCount(amount: Int) {
        val currentDiamonds = _diamondCount.value ?: 0
        _diamondCount.value = currentDiamonds + amount
    }
}