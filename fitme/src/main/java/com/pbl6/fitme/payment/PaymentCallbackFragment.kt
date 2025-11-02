package com.pbl6.fitme.payment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentPaymentCallbackBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class PaymentCallbackFragment : BaseFragment<FragmentPaymentCallbackBinding, PaymentCallbackViewModel>() {
    override fun initView() {
        // Get callback parameters from deep link
        val data = activity?.intent?.data
        if (data != null) {
            val responseCode = data.getQueryParameter("vnp_ResponseCode")
            val orderInfo = data.getQueryParameter("vnp_OrderInfo")
            val transactionStatus = data.getQueryParameter("vnp_TransactionStatus")
            val isProcessed = data.getQueryParameter("processed")?.toBoolean() ?: false
            val status = data.getQueryParameter("status")

            // Show proper UI based on payment result
            if (isProcessed && status == "success") {
                // Use placeholder drawable if specific payment icons are not present
                binding.ivPaymentStatus.setImageResource(R.drawable.ic_splash)
                binding.tvPaymentStatus.text = "Payment Successful!"
                binding.tvPaymentMessage.text = "Your order has been placed successfully."
            } else {
                binding.ivPaymentStatus.setImageResource(R.drawable.ic_splash)
                binding.tvPaymentStatus.text = "Payment Failed"
                binding.tvPaymentMessage.text = "There was an error processing your payment. Please try again."
            }
        }
    }

    override fun initListener() {
        binding.btnContinueShopping.singleClick {
            navigate(R.id.homeFragment)
        }
    }

    override fun initData() {
        // No data to initialize
    }
}