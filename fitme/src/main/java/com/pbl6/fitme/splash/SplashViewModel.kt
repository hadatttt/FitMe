package com.pbl6.fitme.splash

import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SplashViewModel : BaseViewModel() {
    private val _schedulerNotificationData = MutableStateFlow<List<String>>(emptyList())
    val schedulerNotificationData = _schedulerNotificationData.asStateFlow()


    fun getSchedulerNotificationData() {
        _schedulerNotificationData.value = listOf<String>("19:00", "18:00")
    }

}