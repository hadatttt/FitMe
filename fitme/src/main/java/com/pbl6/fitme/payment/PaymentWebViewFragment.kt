package com.pbl6.fitme.payment

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.R

class PaymentWebViewFragment : Fragment() {
    private var webView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_payment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview)
        val progress = view.findViewById<View>(R.id.progress)
        val btnClose = view.findViewById<View>(R.id.btnClose)

        btnClose.setOnClickListener { findNavController().popBackStack() }

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (isCallbackUrl(url)) {
                        try {
                            val uri = Uri.parse(url)
                            val vnpResponseCode = uri.getQueryParameter("vnp_response_code") ?: ""
                            val vnpOrderInfo = uri.getQueryParameter("vnp_order_info") ?: ""
                            val vnpTransactionStatus = uri.getQueryParameter("vnp_transaction_status") ?: ""
                            val vnpTransactionNo = uri.getQueryParameter("vnp_transaction_no")
                            val vnpPayDate = uri.getQueryParameter("vnp_pay_date")

                            // Avoid calling backend callback endpoint directly (may be misconfigured to localhost).
                            // Instead, refresh order status from server (authoritative) and show result based on vnp_response_code.
                            val orderIdArg = arguments?.getString("order_id")
                            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
                            val builder = AlertDialog.Builder(requireContext())

                            if (!token.isNullOrBlank() && !orderIdArg.isNullOrBlank()) {
                                val repo = MainRepository()
                                repo.getOrderById(token, orderIdArg) { order ->
                                    activity?.runOnUiThread {
                                        val success = vnpResponseCode == "00"
                                        builder.setTitle(if (success) "Payment Successful" else "Payment Result")
                                        val serverStatus = order?.orderStatus ?: order?.status
                                        val msg = StringBuilder()
                                        if (success) {
                                            msg.append("Thanh toán thành công. ")
                                        } else {
                                            msg.append("Thanh toán không thành công. Mã trả về: $vnpResponseCode\n")
                                        }
                                        if (!serverStatus.isNullOrBlank()) {
                                            msg.append("Trạng thái đơn trên server: $serverStatus")
                                        }
                                        builder.setMessage(msg.toString())
                                        builder.setPositiveButton("OK") { _, _ ->
                                            try {
                                                val bundle = android.os.Bundle()
                                                bundle.putString("order_status", "confirming")
                                                findNavController().navigate(R.id.ordersFragment, bundle)
                                            } catch (_: Exception) { findNavController().popBackStack() }
                                        }
                                        builder.setCancelable(false)
                                        builder.show()
                                    }
                                }
                            } else {
                                // No token/orderId available — show basic result from query param
                                activity?.runOnUiThread {
                                    val success = vnpResponseCode == "00"
                                    builder.setTitle(if (success) "Payment Successful" else "Payment Result")
                                    val msg = if (success) "Thanh toán thành công." else "Thanh toán không thành công. Mã trả về: $vnpResponseCode"
                                    builder.setMessage(msg)
                                    builder.setPositiveButton("OK") { _, _ -> findNavController().popBackStack() }
                                    builder.setCancelable(false)
                                    builder.show()
                                }
                            }
                        } catch (ex: Exception) {
                            findNavController().popBackStack()
                        }
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progress.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                }
            }
        }

        val paymentUrl = arguments?.getString("payment_url")
        if (!paymentUrl.isNullOrBlank()) {
            webView?.loadUrl(paymentUrl)
        } else {
            // nothing to load, go back
            findNavController().popBackStack()
        }
    }

    private fun isCallbackUrl(url: String): Boolean {
        // The backend's return url contains "/payment/vn-pay-callback"; detect that
        return url.contains("/payment/vn-pay-callback") || url.contains("vnp_response_code")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView?.destroy()
        webView = null
    }
}
