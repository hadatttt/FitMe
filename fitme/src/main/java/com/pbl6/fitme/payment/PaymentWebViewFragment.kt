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
    private var paymentProvider: String? = null // "VNPAY" hoặc "MOMO"

    override fun initView() {
        paymentUrl = arguments?.getString("payment_url")
        paymentProvider = arguments?.getString("payment_provider") // Lấy loại thanh toán nếu cần

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

        // Cho phép zoom nếu cần thiết cho trang thanh toán
        binding.webView.settings.builtInZoomControls = true
        binding.webView.settings.displayZoomControls = false

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                if (url != null) {
                    checkPaymentStatus(url)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // Xử lý riêng cho MoMo nếu nó cố mở app (DeepLink)
                // Link dạng: momo://?action=...
                if (url.startsWith("momo://") || url.startsWith("vietcombank://")) { // v.v..
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        // Người dùng không cài app MoMo, kệ cho nó load web tiếp hoặc thông báo
                    }
                }

                if (checkPaymentStatus(url)) {
                    return true
                }
                return false
            }
        }

        binding.webView.loadUrl(paymentUrl!!)
    }

    /**
     * Kiểm tra URL để xem đã thanh toán xong chưa
     * Return true nếu đã xử lý xong (điều hướng về màn Order)
     */
    private fun checkPaymentStatus(url: String): Boolean {
        // 1. Xử lý VNPay
        if (url.contains("vnpay_return")) {
            handleVNPayResult(url)
            return true
        }

        // 2. Xử lý MoMo
        // MoMo thường trả về URL chứa "resultCode".
        // Backend của bạn setup Redirect URL là gì? Ví dụ: yourdomain.com/momo_return
        // Hoặc đơn giản check chuỗi "resultCode="
        if (url.contains("resultCode=") || url.contains("message=")) {
            // Đôi khi MoMo redirect qua nhiều bước, cần chắc chắn đây là URL cuối cùng từ Backend của bạn
            // Ví dụ backend bạn trả về: .../payment/momo/return?resultCode=0...
            if (url.contains("momo_return") || url.contains("resultCode")) {
                handleMomoResult(url)
                return true
            }
        }

        return false
    }

    private fun handleVNPayResult(url: String) {
        val bundle = Bundle()
        if (url.contains("vnp_ResponseCode=00")) {
            Toast.makeText(requireContext(), "Thanh toán VNPay thành công!", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "processing")
        } else {
            Toast.makeText(requireContext(), "Thanh toán VNPay thất bại", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "pending")
        }
        navigate(R.id.ordersFragment, bundle)
    }

    private fun handleMomoResult(url: String) {
        val bundle = Bundle()
        // MoMo: resultCode=0 là thành công
        if (url.contains("resultCode=0")) {
            Toast.makeText(requireContext(), "Thanh toán MoMo thành công!", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "processing")
        } else {
            // resultCode=9000 (User cancel) hoặc mã lỗi khác
            Toast.makeText(requireContext(), "Thanh toán MoMo thất bại hoặc bị hủy", Toast.LENGTH_LONG).show()
            bundle.putString("order_status", "pending")
        }
        navigate(R.id.ordersFragment, bundle)
    }

    override fun initData() {}
    override fun initListener() {}
}