package com.pbl6.fitme.payment

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
        val btnRefresh = view.findViewById<View>(R.id.btnRefresh)

        btnClose.setOnClickListener { findNavController().popBackStack() }

        btnRefresh.setOnClickListener {
            // Manual refresh of order status
            val orderIdArg = arguments?.getString("order_id")
            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank() || orderIdArg.isNullOrBlank()) {
                android.widget.Toast.makeText(requireContext(), "No order to refresh", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val repo = MainRepository()
                repo.getOrderById(token, orderIdArg) { order ->
                    activity?.runOnUiThread {
                        if (order != null) {
                            try {
                                val builder = AlertDialog.Builder(requireContext())
                                val serverStatus = order.orderStatus ?: order.status
                                builder.setTitle("Order status")
                                builder.setMessage("Current order status: ${serverStatus ?: "unknown"}")
                                builder.setPositiveButton("OK") { _, _ -> findNavController().popBackStack() }
                                builder.setCancelable(false)
                                builder.show()
                            } catch (_: Exception) { }
                        } else {
                            android.widget.Toast.makeText(requireContext(), "Failed to fetch order status", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false

                    // Handle non-http(s) schemes (e.g. momo:// or custom app deep links)
                    val scheme = uri.scheme
                    if (!scheme.isNullOrBlank() && scheme != "http" && scheme != "https") {
                        try {
                            // Special-case Android intent:// URIs
                            if (scheme.equals("intent", ignoreCase = true)) {
                                try {
                                    val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (intent.resolveActivity(requireContext().packageManager) != null) {
                                        requireContext().startActivity(intent)
                                    } else {
                                        // No activity to handle intent:// — show fallback dialog
                                        showAppNotInstalledDialog(uri.toString())
                                    }
                                } catch (iex: Exception) {
                                    showAppNotInstalledDialog(uri.toString())
                                }
                                return true
                            }

                            // General custom-scheme handler (e.g. momo://)
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (intent.resolveActivity(requireContext().packageManager) != null) {
                                requireContext().startActivity(intent)
                            } else {
                                showAppNotInstalledDialog(uri.toString())
                            }
                        } catch (ex: Exception) {
                            try {
                                // Last-resort: try opening in browser
                                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                requireContext().startActivity(browserIntent)
                            } catch (ie: Exception) {
                                // give up silently and pop back
                                activity?.runOnUiThread { android.widget.Toast.makeText(requireContext(), "Không thể xử lý liên kết: ${uri}", android.widget.Toast.LENGTH_LONG).show() }
                            }
                        }
                        return true
                    }

                    val url = uri.toString()

                    if (isCallbackUrl(url)) {
                        try {
                            val uri = Uri.parse(url)

                            // VNPay params
                            val vnpResponseCode = uri.getQueryParameter("vnp_response_code") ?: ""
                            // Momo params (resultCode is used by backend/ipn)
                            val momoResultCode = uri.getQueryParameter("resultCode") ?: uri.getQueryParameter("errorCode") ?: ""

                            val orderIdArg = arguments?.getString("order_id")
                            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
                            val builder = AlertDialog.Builder(requireContext())

                            // Determine success: VNPay uses "00", Momo uses "0"
                            val success = (vnpResponseCode == "00") || (momoResultCode == "0")

                            if (!token.isNullOrBlank() && !orderIdArg.isNullOrBlank()) {
                                val repo = MainRepository()
                                repo.getOrderById(token, orderIdArg) { order ->
                                    activity?.runOnUiThread {
                                        builder.setTitle(if (success) "Payment Successful" else "Payment Result")
                                        val serverStatus = order?.orderStatus ?: order?.status
                                        val msg = StringBuilder()
                                        if (success) {
                                            msg.append("Thanh toán thành công. ")
                                        } else {
                                            // prefer showing Momo or VNPay code if available
                                            val code = if (vnpResponseCode.isNotBlank()) vnpResponseCode else momoResultCode
                                            msg.append("Thanh toán không thành công. Mã trả về: $code\n")
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
                                // No token/orderId available — show basic result from query params
                                activity?.runOnUiThread {
                                    builder.setTitle(if (success) "Payment Successful" else "Payment Result")
                                    val code = if (vnpResponseCode.isNotBlank()) vnpResponseCode else momoResultCode
                                    val msg = if (success) "Thanh toán thành công." else "Thanh toán không thành công. Mã trả về: $code"
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

                    // Detect VNPay 'merchant not approved' error by checking page text
                    try {
                        view?.evaluateJavascript("(function(){return (document.body && document.body.innerText) || (document.documentElement && document.documentElement.innerText) || '';})()") { value ->
                            try {
                                val pageText = value?.trim('"')?.replace("\\n", "\n") ?: ""
                                if (pageText.contains("Website này chưa được phê duyệt", ignoreCase = true) || pageText.contains("chưa được phê duyệt", ignoreCase = true)) {
                                    activity?.runOnUiThread {
                                        try {
                                            val builder = AlertDialog.Builder(requireContext())
                                            builder.setTitle("VNPay Error")
                                            builder.setMessage("VNPay trả về: Website/merchant chưa được phê duyệt. Vui lòng kiểm tra cấu hình merchant (vnp_TmnCode / return URL) hoặc liên hệ VNPay để được whitelist.")
                                            builder.setPositiveButton("OK") { _, _ -> }
                                            builder.setCancelable(true)
                                            builder.show()
                                        } catch (_: Exception) { }
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }
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

    private fun showAppNotInstalledDialog(uri: String) {
        activity?.runOnUiThread {
            try {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Ứng dụng cần thiết không được cài đặt")
                builder.setMessage("Ứng dụng thanh toán (ví dụ MoMo) không được tìm thấy để xử lý liên kết. Bạn có thể sao chép liên kết và mở thủ công hoặc cài đặt ứng dụng tương ứng.")
                builder.setPositiveButton("Sao chép liên kết") { _, _ ->
                    try {
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("payment_link", uri)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(requireContext(), "Đã sao chép liên kết vào clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (ex: Exception) {
                        android.widget.Toast.makeText(requireContext(), "Không thể sao chép liên kết", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNeutralButton("Mở trình duyệt") { _, _ ->
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        requireContext().startActivity(browserIntent)
                    } catch (ex: Exception) {
                        android.widget.Toast.makeText(requireContext(), "Không thể mở liên kết bằng trình duyệt", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("Huỷ") { _, _ -> }
                builder.setCancelable(true)
                builder.show()
            } catch (ex: Exception) {
                // fallback toast
                android.widget.Toast.makeText(requireContext(), "Ứng dụng không được tìm thấy và không thể hiển thị dialog.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isCallbackUrl(url: String): Boolean {
        // Detect VNPay and MoMo callback return URLs or query params
        return url.contains("/payment/vn-pay-callback") || url.contains("vnp_response_code") || url.contains("resultCode") || url.contains("errorCode")
    }

    override fun onResume() {
        super.onResume()
        // If user returned from external payment app, allow auto-refresh of order status
        val orderIdArg = arguments?.getString("order_id")
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank() && !orderIdArg.isNullOrBlank()) {
            val repo = MainRepository()
            repo.getOrderById(token, orderIdArg) { order ->
                activity?.runOnUiThread {
                    if (order != null) {
                        // If order is confirmed, show dialog and navigate to orders
                        val serverStatus = order.orderStatus ?: order.status
                        if (!serverStatus.isNullOrBlank() && serverStatus.equals("CONFIRMED", ignoreCase = true)) {
                            try {
                                val builder = AlertDialog.Builder(requireContext())
                                builder.setTitle("Payment Successful")
                                builder.setMessage("Thanh toán thành công. Trạng thái đơn: $serverStatus")
                                builder.setPositiveButton("OK") { _, _ ->
                                    try {
                                        val bundle = android.os.Bundle()
                                        bundle.putString("order_status", "confirming")
                                        findNavController().navigate(R.id.ordersFragment, bundle)
                                    } catch (_: Exception) { findNavController().popBackStack() }
                                }
                                builder.setCancelable(false)
                                builder.show()
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView?.destroy()
        webView = null
    }
}
