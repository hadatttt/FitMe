package com.pbl6.fitme.checkout

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentPaymentWebViewBinding
import com.pbl6.fitme.home.HomeMainViewModel
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate

class PaymentWebViewFragment : BaseFragment<FragmentPaymentWebViewBinding, HomeMainViewModel>() {

    private var paymentUrl: String? = null
    private var paymentProvider: String? = null

    override fun initView() {
        paymentUrl = arguments?.getString("payment_url")
        paymentProvider = arguments?.getString("payment_provider")

        if (paymentUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Lỗi đường dẫn thanh toán", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.builtInZoomControls = true
        binding.webView.settings.displayZoomControls = false

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.startsWith("momo://") || url.startsWith("vietcombank://") || url.startsWith("vnpay://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return false
                    }
                }

                if (url.contains("vnp_ResponseCode") || url.contains("vn-pay-callback")) {
                    handleVNPayResult(url)
                    return true
                }

                if (url.contains("resultCode=") || url.contains("momo_return")) {
                    handleMomoResult(url)
                    return true
                }

                return false
            }
        }

        binding.webView.loadUrl(paymentUrl!!)
    }

    private fun handleVNPayResult(url: String) {
        val bundle = Bundle()
        if (url.contains("vnp_ResponseCode=00")) {
            Toast.makeText(requireContext(), "Thanh toán VNPay thành công!", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "processing")
        } else {
            Toast.makeText(requireContext(), "Thanh toán thất bại hoặc đã hủy", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "pending")
        }
        navigate(R.id.ordersFragment, bundle)
    }

    private fun handleMomoResult(url: String) {
        val bundle = Bundle()
        if (url.contains("resultCode=0")) {
            Toast.makeText(requireContext(), "Thanh toán MoMo thành công!", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "processing")
        } else {
            Toast.makeText(requireContext(), "Thanh toán MoMo thất bại", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "pending")
        }
        navigate(R.id.ordersFragment, bundle)
    }

    override fun initData() {}
    override fun initListener() {}
}