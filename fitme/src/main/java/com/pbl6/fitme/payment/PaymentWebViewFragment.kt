package com.pbl6.fitme.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.session.SessionManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

class PaymentWebViewFragment : Fragment() {

    private var webView: WebView? = null
    private var vnpTxnRef: String? = null   // ← biến quan trọng
    private var injectedQrUrl: String? = null
    private var injectedAmountText: String? = null
    private var qrRequested = false
    private var paymentMethodArg: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_payment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview)
        val progress = view.findViewById<View>(R.id.progress)
        val btnClose = view.findViewById<View>(R.id.btnClose)
        val btnRefresh = view.findViewById<View>(R.id.btnRefresh)

        btnClose.setOnClickListener { findNavController().popBackStack() }
        btnRefresh.setOnClickListener { manualRefresh() }

        setupWebView(progress)

        val paymentUrl = arguments?.getString("payment_url")
        injectedQrUrl = arguments?.getString("qr_url")
        injectedAmountText = arguments?.getString("amount_text")
        paymentMethodArg = arguments?.getString("payment_method")
        if (!paymentUrl.isNullOrBlank()) {
            webView?.loadUrl(paymentUrl)
        } else findNavController().popBackStack()
    }

    // ----------------------------------------------------------------------
    // 1. SETUP WEBVIEW CHUẨN
    // ----------------------------------------------------------------------

    private fun setupWebView(progress: View) {
        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()

            // Thêm: Cho phép tải nội dung hỗn hợp (để tải ảnh QR từ HTTPS trên trang HTTPS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false

                    // 1) Handle deep link momo:// or intent://
                    if (handleDeepLink(uri)) return true

                    // 2) Handle VNPay/MoMo callback URL
                    if (isCallbackUrl(uri.toString())) {
                        handlePaymentCallback(uri)
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progress.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                    
                    val initialPaymentUrl = arguments?.getString("payment_url")
                    
                    if (paymentMethodArg == "MOMO" && !injectedQrUrl.isNullOrBlank()) {
                        // If this is a Momo payment and we have a QR, show the standalone page if requested
                        if (initialPaymentUrl == "about:blank") {
                            showStandaloneQrPage(view, injectedQrUrl!!, injectedAmountText ?: "")
                        } else {
                            showOverlayOnMerchant(view, injectedQrUrl!!, injectedAmountText ?: "")
                        }
                    } else {
                        // For non-MOMO (e.g. VNPay) or when QR missing, try to request QR only if payment method is MOMO
                        val orderId = arguments?.getString("order_id")
                        if (paymentMethodArg == "MOMO" && !qrRequested && !orderId.isNullOrBlank()) {
                            qrRequested = true
                            requestQrFromServer(orderId, view)
                        }
                    }
                    detectVNPayMerchantNotApproved(view)
                }
            }
        }
    }

    private fun requestQrFromServer(orderId: String, webView: WebView?) {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val email = SessionManager.getInstance().getUserEmail(requireContext())
        if (token.isNullOrBlank()) return

        MainRepository().getOrderById(token, orderId) { order ->
            val amountVnd = try {
                val total = order?.totalAmount ?: order?.subtotal ?: return@getOrderById
                val exchangeRate = 25000L
                BigDecimal(total.toString())
                    .multiply(BigDecimal.valueOf(exchangeRate))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            } catch (e: Exception) { null }

            if (amountVnd == null) return@getOrderById

            MainRepository().createMomoPayment(token, amountVnd, email ?: "", orderId) { momoResp ->
                activity?.runOnUiThread {
                    if (momoResp == null) return@runOnUiThread

                    val qr = momoResp.qrCodeUrl
                    val pay = momoResp.payUrl
                    val deeplink = momoResp.deeplink

                    if (!qr.isNullOrBlank()) {
                        injectedQrUrl = qr
                        injectedAmountText = "Amount (VND): $amountVnd"
                        // Chạy lại logic showOverlayOnMerchant vì QR đã có
                        showOverlayOnMerchant(webView, qr, injectedAmountText ?: "")
                    } else if (!pay.isNullOrBlank()) {
                        try {
                            val bundle = Bundle()
                            bundle.putString("payment_url", pay)
                            bundle.putString("order_id", orderId)
                            findNavController().navigate(R.id.paymentWebViewFragment, bundle)
                        } catch (_: Exception) {
                            try {
                                requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pay)))
                            } catch (_: Exception) { }
                        }
                    } else if (!deeplink.isNullOrBlank()) {
                        try {
                            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deeplink)))
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // 1.1 INJECT QR CARD VÀO TRANG THANH TOÁN (INLINE INJECTION)
    // ----------------------------------------------------------------------

    private fun showOverlayOnMerchant(view: WebView?, qrUrl: String, amountText: String) {
        try {
            val displayQrUrl = if (qrUrl.startsWith("http") || qrUrl.startsWith("data:")) {
                qrUrl
            } else {
                try {
                    URLEncoder.encode(qrUrl, "UTF-8")
                        .let { "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$it" }
                } catch (_: Exception) {
                    qrUrl
                }
            }
            
            val safeQr = displayQrUrl.replace("'", "\\'")
            val safeAmount = amountText.replace("'", "\\'")
            val safeId = "injected_qr_inline_card"
            
            // Lấy Order ID để hiển thị thêm
            val orderId = arguments?.getString("order_id") ?: ""
            val safeOrder = orderId.replace("'", "\\'")

            val js = """
                (function(){
                  try{
                    if(document.getElementById('$safeId')) return;
                    
                    // --- CSS STYLE (Màu MoMo) ---
                    var style = document.createElement('style');
                    style.type = 'text/css';
                    style.innerHTML = `
                        .inline-qr-wrapper {
                            display: flex;
                            justify-content: center;
                            width: 100%;
                            box-sizing: border-box;
                            margin: 18px 0;
                        }
                        .inline-qr-card {
                            width: 100%;
                            max-width: 460px;
                            background: #ffffff;
                            border-radius: 14px;
                            padding: 20px;
                            text-align: center;
                            box-shadow: 0 12px 36px rgba(15,23,42,0.08);
                        }
                        .inline-qr-card img {
                            width: 280px; 
                            max-width: 80%; 
                            height: auto; 
                            border-radius: 10px; 
                            margin-top: 10px;
                            border: 3px solid #D82D8B; /* Màu MoMo */
                        }
                        .inline-qr-card .amount {
                            margin-top: 12px;
                            font-size: 17px;
                            color: #0b1221;
                            font-weight: 700;
                        }
                        .inline-qr-card .order {
                            margin-top: 4px;
                            font-size: 13px;
                            color: #757b86;
                        }
                        .inline-qr-card .hint {
                            margin-top: 10px;
                            font-size: 13px;
                            color: #6b7280;
                        }
                    `;
                    document.head.appendChild(style);

                    // --- HTML CONTENT ---
                    var wrapper = document.createElement('div');
                    wrapper.id = '$safeId';
                    wrapper.className = 'inline-qr-wrapper';
                    
                    var card = document.createElement('div');
                    card.className = 'inline-qr-card';
                    
                    card.innerHTML = `
                        <div style="font-size: 18px; font-weight: 700; color: #0b1221;">Quét Mã QR</div>
                        <img src="${safeQr}" alt="QR Code" />
                        <div class="amount">${safeAmount}</div>
                        <div class="order">Mã đơn: ${safeOrder}</div>
                        <div class="hint">Sử dụng ứng dụng MoMo hoặc các ví hỗ trợ QR Code</div>
                    `;
                    
                    wrapper.appendChild(card);
                    
                    // --- FIND INSERTION POINT (Chèn sau nút Thanh toán) ---
                    var insertTarget = document.body;
                    var btns = Array.from(document.querySelectorAll('button, a'));
                    var lastMomoButton = btns.find(function(b) {
                        var t = (b.textContent || '').toLowerCase();
                        return t.includes('momo') || t.includes('thanh toán bằng ví');
                    });
                    
                    if (lastMomoButton && lastMomoButton.parentElement) {
                        insertTarget = lastMomoButton.parentElement;
                    }

                    insertTarget.appendChild(wrapper);
                    try{ wrapper.scrollIntoView({behavior:'smooth', block:'center'}); }catch(e){}
                  }catch(e){
                    // Optional: Fallback to fixed overlay if inline fails dramatically
                  }
                })();
            """.trimIndent()

            view?.evaluateJavascript(js, null)
        } catch (_: Exception) {
        }
    }


    // ----------------------------------------------------------------------
    // 1.2 HIỂN THỊ TRANG QR ĐỘC LẬP (about:blank)
    // ----------------------------------------------------------------------

    private fun showStandaloneQrPage(view: WebView?, qrUrl: String, amountText: String) {
        try {
            val displayQrUrl = if (qrUrl.startsWith("http") || qrUrl.startsWith("data:")) {
                qrUrl
            } else {
                try {
                    URLEncoder.encode(qrUrl, "UTF-8")
                        // Tăng size ảnh QR để hiển thị rõ hơn trên trang độc lập
                        .let { "https://api.qrserver.com/v1/create-qr-code/?size=600x600&data=$it" }
                } catch (_: Exception) {
                    qrUrl
                }
            }

            val safeQr = displayQrUrl.replace("'", "\\'")
            val safeAmount = amountText.replace("'", "\\'")
            val orderId = arguments?.getString("order_id") ?: ""
            val safeOrder = orderId.replace("'", "\\'")

            val html = """
                <!doctype html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width,initial-scale=1" />
                  <style>
                    :root{ --momo-red: #D82D8B; --momo-grad-from:#ff2d55; --momo-grad-to:#ff6b88; --bg: #f6f7fb; --card:#ffffff }
                    html,body{ height:100%; margin:0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial; background:var(--bg); }
                    .wrap{ min-height:100%; display:flex; align-items:center; justify-content:center; padding:24px }
                    .card{ width:100%; max-width:460px; background:var(--card); border-radius:14px; padding:20px 20px 28px; box-shadow:0 18px 48px rgba(12,20,40,0.12); text-align:center }
                    .brand{ display:flex; align-items:center; gap:12px; justify-content:center }
                    .logo{ width:48px; height:48px; border-radius:12px; background:linear-gradient(135deg,var(--momo-grad-from),var(--momo-grad-to)); display:flex; align-items:center; justify-content:center; color:#fff; font-weight:800 }
                    .title{ font-size:18px; font-weight:700; color:#0b1221 }
                    .qr{ margin-top:18px; width:360px; max-width:84%; height:auto; border-radius:16px; background:#fff; padding:10px; box-shadow:0 8px 20px rgba(0,0,0,0.06); border: 3px solid var(--momo-red); }
                    .amount{ margin-top:14px; font-size:17px; color:#0b1221; font-weight:700 }
                    .order{ margin-top:6px; font-size:13px; color:#757b86 }
                    .actions{ margin-top:18px; display:flex; gap:12px; justify-content:center }
                    .btn{ padding:12px 16px; border-radius:12px; font-weight:700; border:none; cursor:pointer; font-size:14px }
                    .btn-primary{ background:linear-gradient(90deg,var(--momo-grad-from),var(--momo-grad-to)); color:#fff; box-shadow:0 8px 24px rgba(255,45,85,0.18) }
                    .btn-ghost{ background:#ffffff; border:1px solid #eef2f6; color:#0b1221 }
                    .hint{ margin-top:12px; font-size:13px; color:#6b7280 }
                    @media (max-width:420px){ .qr{ width:280px } }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="brand">
                        <div class="logo">MoMo</div>
                        <div class="title">Thanh toán bằng MoMo</div>
                      </div>
                      <img class="qr" src="$safeQr" alt="QR Code" />
                      <div class="amount">$safeAmount</div>
                      <div class="order">Mã đơn: $safeOrder</div>
                      <div class="actions">
                        <button class="btn btn-primary" onclick="window.Android && window.Android.open('$qrUrl')">Mở bằng MoMo</button>
                        <button class="btn btn-ghost" onclick="window.Android && window.Android.copy('$qrUrl')">Sao chép liên kết</button>
                      </div>
                      <div class="hint">Quét mã bằng ứng dụng MoMo hoặc nhấn Mở để tiếp tục</div>
                    </div>
                  </div>
                </body>
                </html>
            """.trimIndent()

            // Enable a JS interface for actions (open, copy, close)
            view?.settings?.javaScriptEnabled = true
            view?.addJavascriptInterface(object {
                @JavascriptInterface
                fun close() { activity?.runOnUiThread { findNavController().popBackStack() } }

                @JavascriptInterface
                fun copy(text: String) {
                    try {
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("payment_link", text)
                        clipboard.setPrimaryClip(clip)
                        activity?.runOnUiThread { Toast.makeText(requireContext(), "Đã copy liên kết", Toast.LENGTH_SHORT).show() }
                    } catch (_: Exception) {}
                }

                @JavascriptInterface
                fun open(url: String) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (intent.resolveActivity(requireContext().packageManager) != null) {
                            requireContext().startActivity(intent)
                        } else {
                            activity?.runOnUiThread { Toast.makeText(requireContext(), "MoMo app not found", Toast.LENGTH_SHORT).show() }
                        }
                    } catch (_: Exception) {
                        activity?.runOnUiThread { Toast.makeText(requireContext(), "Không thể mở liên kết", Toast.LENGTH_SHORT).show() }
                    }
                }
            }, "Android")

            view?.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } catch (_: Exception) {
        }
    }
    
    // ----------------------------------------------------------------------
    // PHẦN CÒN LẠI GIỮ NGUYÊN
    // ----------------------------------------------------------------------
    
    private fun handleDeepLink(uri: Uri): Boolean {
        // ... (Giữ nguyên)
        val scheme = uri.scheme ?: return false
        if (scheme == "http" || scheme == "https") return false

        return try {
            if (scheme == "intent") {
                val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(requireContext().packageManager) != null)
                    requireContext().startActivity(intent)
                else showAppNotInstalledDialog(uri.toString())
                return true
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(requireContext().packageManager) != null)
                requireContext().startActivity(intent)
            else showAppNotInstalledDialog(uri.toString())

            true
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Không thể mở liên kết", Toast.LENGTH_LONG).show()
            true
        }
    }

    private fun handlePaymentCallback(uri: Uri) {
        // ... (Giữ nguyên)
        try {
            val vnpResponseCode = uri.getQueryParameter("vnp_response_code") ?: ""
            vnpTxnRef = uri.getQueryParameter("vnp_TxnRef") ?: ""    // ← LƯU ORDER ID
            val momoResultCode = uri.getQueryParameter("resultCode")
                ?: uri.getQueryParameter("errorCode") ?: ""

            val success = (vnpResponseCode == "00") || (momoResultCode == "0")

            val token = SessionManager.getInstance().getAccessToken(requireContext())
            val orderId = vnpTxnRef ?: arguments?.getString("order_id")

            if (!token.isNullOrBlank() && !orderId.isNullOrBlank()) fetchOrderStatusFromServer(orderId, token, success, vnpResponseCode, momoResultCode)
            else showResultDialog(success, vnpResponseCode, momoResultCode)

        } catch (_: Exception) {
            findNavController().popBackStack()
        }
    }

    private fun fetchOrderStatusFromServer(
        orderId: String,
        token: String,
        success: Boolean,
        vnpCode: String,
        momoCode: String
    ) {
        // ... (Giữ nguyên)
        MainRepository().getOrderById(token, orderId) { order ->
            activity?.runOnUiThread {
                val builder = AlertDialog.Builder(requireContext())
                val serverStatus = order?.orderStatus ?: order?.status
                val msg = StringBuilder()

                builder.setTitle(if (success) "Payment Successful" else "Payment Failed")

                if (!success) {
                    val code = if (vnpCode.isNotBlank()) vnpCode else momoCode
                    msg.append("Mã lỗi: $code\n")
                }

                if (!serverStatus.isNullOrBlank()) msg.append("Trạng thái đơn: $serverStatus")

                builder.setMessage(msg.toString())
                builder.setPositiveButton("OK") { _, _ ->
                    navigateToOrders()
                }
                builder.setCancelable(false)
                builder.show()
            }
        }
    }

    private fun showResultDialog(success: Boolean, vnpCode: String, momoCode: String) {
        // ... (Giữ nguyên)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (success) "Payment Successful" else "Payment Failed")

        val code = if (vnpCode.isNotBlank()) vnpCode else momoCode
        builder.setMessage(if (success) "Thanh toán thành công." else "Thanh toán thất bại. Mã lỗi: $code")

        builder.setPositiveButton("OK") { _, _ -> findNavController().popBackStack() }
        builder.setCancelable(false)
        builder.show()
    }

    private fun detectVNPayMerchantNotApproved(view: WebView?) {
        // ... (Giữ nguyên)
        view?.evaluateJavascript(
            "(function(){return document.body.innerText;})()"
        ) { value ->
            val text = value?.trim('"')?.replace("\\n", "\n") ?: ""
            if (text.contains("chưa được phê duyệt", ignoreCase = true)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("VNPay Error")
                    .setMessage("Website/Merchant chưa được phê duyệt. Kiểm tra cấu hình TMNCode hoặc liên hệ VNPay.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun manualRefresh() {
        // ... (Giữ nguyên)
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val orderId = vnpTxnRef ?: arguments?.getString("order_id")

        if (token.isNullOrBlank() || orderId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Không có đơn để refresh", Toast.LENGTH_SHORT).show()
            return
        }

        MainRepository().getOrderById(token, orderId) { order ->
            activity?.runOnUiThread {
                AlertDialog.Builder(requireContext())
                    .setTitle("Order status")
                    .setMessage("Trạng thái hiện tại: ${order?.orderStatus ?: order?.status ?: "unknown"}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        // ... (Giữ nguyên)
        super.onResume()

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val orderId = vnpTxnRef ?: arguments?.getString("order_id")

        if (token.isNullOrBlank() || orderId.isNullOrBlank()) return

        MainRepository().getOrderById(token, orderId) { order ->
            activity?.runOnUiThread {
                val status = order?.orderStatus ?: order?.status ?: return@runOnUiThread
                if (status.equals("CONFIRMED", ignoreCase = true)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Payment Successful")
                        .setMessage("Đơn hàng đã được xác nhận.")
                        .setPositiveButton("OK") { _, _ -> navigateToOrders() }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun navigateToOrders() {
        // ... (Giữ nguyên)
        try {
            val bundle = Bundle()
            bundle.putString("order_status", "confirming")
            findNavController().navigate(R.id.ordersFragment, bundle)
        } catch (_: Exception) {
            findNavController().popBackStack()
        }
    }

    private fun isCallbackUrl(url: String): Boolean =
        url.contains("vnp_response_code") || url.contains("vnp_TxnRef") ||
                url.contains("resultCode") || url.contains("errorCode")

    private fun showAppNotInstalledDialog(uri: String) {
        // ... (Giữ nguyên)
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Ứng dụng không có sẵn")
            .setMessage("Ứng dụng thanh toán không được cài đặt. Bạn muốn làm gì?")
            .setPositiveButton("Sao chép liên kết") { _, _ ->
                val clip = ClipData.newPlainText("payment_link", uri)
                val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Đã sao chép", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Mở trong trình duyệt") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                requireContext().startActivity(intent)
            }
            .setNegativeButton("Hủy", null)

        builder.show()
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }
}