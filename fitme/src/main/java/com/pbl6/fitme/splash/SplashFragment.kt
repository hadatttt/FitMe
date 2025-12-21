package com.pbl6.fitme.splash

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.pbl6.fitme.R
import com.pbl6.fitme.checkin.CheckInDialogFragment
import com.pbl6.fitme.databinding.FragmentSplashBinding
import com.pbl6.fitme.service.AppPreferences
import com.pbl6.fitme.service.NotificationHelper
import com.pbl6.fitme.service.NotificationWorker
import com.pbl6.fitme.untils.AppSharePref
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.collectLatestFlow
import hoang.dqm.codebase.utils.singleClick

class SplashFragment : BaseFragment <FragmentSplashBinding, SplashViewModel>() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (Build.VERSION.SDK_INT >= 33) {
            if (!isGranted) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    NotificationHelper.showSettingsDialog(requireActivity())
                }
            } else {
                getNotificationDailyConfig()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun initView() {
        checkNotificationPermission()
        registerNotification()
    }

    private fun getNotificationDailyConfig() {
        viewModel.getSchedulerNotificationData()
    }

    private fun checkNotificationPermission() {
        NotificationHelper.checkPermission(
            fragment = this,
            onGranted = { getNotificationDailyConfig() },
            permissionLauncher = notificationPermissionLauncher
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerNotification() {
        AppPreferences.updateLastAppOpenDate(requireContext())
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TAG", "Token hiện tại: $token")
            } else {
                Log.e("FCM_TAG", "Lỗi lấy token", task.exception)
            }
        }
        collectLatestFlow(viewModel.schedulerNotificationData) { data ->
            if (data.isEmpty()) return@collectLatestFlow
            NotificationWorker.scheduleDailyNotifications(
                requireContext(),
                data
            )
        }
    }
    override fun initListener() {
        binding.ivRegister.singleClick {
            navigate(R.id.registerFragment)
        }
        binding.ivNextLogin.singleClick {
            navigate(R.id.loginFragment)
        }
    }
    override fun initData() {

    }
}


