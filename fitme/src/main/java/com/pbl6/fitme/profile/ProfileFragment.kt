package com.pbl6.fitme.profile

import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProfileBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.repository.CouponRepository
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.RecommendRepository
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import com.pbl6.fitme.untils.AppConstrain
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    // Khai báo các Repository
    private val mainRepository = MainRepository()
    private val recommendRepository = RecommendRepository()
    private val userRepository = UserRepository()
    private val couponRepository = CouponRepository() // 1. Thêm Repo Coupon

    private lateinit var productAdapter: ProductAdapter

    override fun initView() {
        // Setup Toolbar và Tab
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.person_id)
        fetchUserPoints()
        // Setup RecyclerView sản phẩm
        setupRecyclerViews()
        updateOrderBadges()
        fetchUserProfile()
        fetchVoucherCount() // 2. Gọi hàm lấy số lượng Voucher
    }



    override fun initListener() {
        binding.llVoucher.singleClick {
            navigate(R.id.voucherFragment)
        }
        // Nút Back
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // Nút Cart kéo thả
        binding.flCart.setDraggableWithClick {
            navigate(R.id.cartFragment)
        }

        // Nút Setting
        binding.btnSetting.singleClick {
            navigate(R.id.settingsFragment)
        }

        // Nút Detail Order
        binding.detail.singleClick {
            navigateToOrder("pending")
        }

        // Logic Bottom Navigation
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            navigate(R.id.homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            navigate(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)
        }

        // Click vào các trạng thái đơn hàng
        binding.llStatusPending.singleClick { navigateToOrder("pending") }
        binding.llStatusConfirmed.singleClick { navigateToOrder("confirmed") }
        binding.llStatusProcessing.singleClick { navigateToOrder("processing") }
        binding.llStatusShipped.singleClick { navigateToOrder("shipped") }
        binding.llStatusDelivered.singleClick { navigateToOrder("delivered") }
        binding.llStatusCancelled.singleClick { navigateToOrder("cancelled") }
    }

    // --- 3. LOGIC LẤY SỐ LƯỢNG VOUCHER (MỚI) ---
    private fun fetchVoucherCount() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank()) {
            couponRepository.getAllCoupons(token) { coupons ->
                activity?.runOnUiThread {
                    // Kiểm tra null binding để tránh crash nếu fragment đã đóng
                    if (context != null && isAdded) {
                        if (coupons != null) {
                            // Chỉ đếm các mã đang hoạt động (isActive = true)
                            val activeCount = coupons.count { it.isActive }
                            // Set text cho TextView tv_voucher_count (trong XML bạn đã thêm ID này)
                            binding.tvVoucher?.text = "$activeCount active >"
                        } else {
                            binding.tvVoucher?.text = "0 active >"
                        }
                    }
                }
            }
        }
    }

    // --- 4. LOGIC LẤY THÔNG TIN USER (Đã chuẩn hóa URL) ---
    private fun fetchUserProfile() {
        val session = SessionManager.getInstance()
        val token = session.getAccessToken(requireContext())
        val userId = session.getUserId(requireContext())?.toString()

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            userRepository.getUserDetail(token, userId) { user ->
                activity?.runOnUiThread {
                    user?.let { userInfo ->
                        // 1. Cập nhật tên
                        binding.txtHello.text = "Hello, ${userInfo.fullName ?: "User"}!"

                        // 2. Cập nhật Avatar
                        userInfo.avatarUrl?.let { url ->
                            // --- SỬ DỤNG APP CONSTRAIN ĐỂ TRÁNH LỖI PATH ---
                            val domain = AppConstrain.DOMAIN_URL.removeSuffix("/")
                            val cleanPath = if (url.startsWith("/")) url.substring(1) else url
                            val finalUrl = if (url.startsWith("http")) url else "$domain/$cleanPath"

                            val glideUrl = GlideUrl(
                                finalUrl,
                                LazyHeaders.Builder()
                                    .addHeader("Authorization", "Bearer $token")
                                    .build()
                            )

                            Glide.with(requireContext())
                                .load(glideUrl)
                                .placeholder(R.drawable.image)
                                .error(R.drawable.image)
                                .circleCrop()
                                .into(binding.imgAvatar)
                        }
                    }
                }
            }
        }
    }
    private fun fetchUserPoints() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()

        // LOG DEBUG
        Log.d("ProfileDebug", "Token: $token")
        Log.d("ProfileDebug", "UserId: $userId")

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            userRepository.getUserPoints(token, userId) { points ->
                Log.d("ProfileDebug", "Points API Response: $points") // Xem API trả về gì
                activity?.runOnUiThread {
                    if (context != null && isAdded) {
                        val currentPoints = points ?: 0
                        binding.tvCoinValue.text = "$currentPoints points"
                    }
                }
            }
        } else {
            Log.e("ProfileDebug", "Không lấy được UserId hoặc Token")
        }
    }
    // --- 5. LOGIC BADGE ĐƠN HÀNG ---
    private fun updateOrderBadges() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val email = SessionManager.getInstance().getUserEmail(requireContext())

        if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
            mainRepository.getOrdersByUser(token, email, null) { orders ->
                activity?.runOnUiThread {
                    if (orders != null) {
                        fun setBadge(textView: android.widget.TextView, statusKey: String) {
                            val count = orders.count {
                                (it.status ?: it.orderStatus ?: "").equals(statusKey, ignoreCase = true)
                            }
                            if (count > 0) {
                                textView.text = count.toString()
                                textView.visibility = View.VISIBLE
                            } else {
                                textView.visibility = View.GONE
                            }
                        }

                        setBadge(binding.tvCountPending, "pending")
                        setBadge(binding.tvCountConfirmed, "confirmed")
                        setBadge(binding.tvCountProcessing, "processing")
                        setBadge(binding.tvCountShipped, "shipped")
                        setBadge(binding.tvCountDelivered, "delivered")
                        setBadge(binding.tvCountCancelled, "cancelled")
                    }
                }
            }
        }
    }

    // --- 6. CÁC HÀM HỖ TRỢ KHÁC ---
    private fun navigateToOrder(status: String) {
        val bundle = android.os.Bundle().apply { putString("order_status", status) }
        navigate(R.id.ordersFragment, bundle)
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
            }
        }
    }

    private fun setupRecyclerViews() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (!token.isNullOrBlank()) {
            productAdapter = ProductAdapter()
            binding.rvTopProducts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvTopProducts.adapter = productAdapter

            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }

            val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()

            val loadDataCallback: (List<Product>?) -> Unit = { list ->
                activity?.runOnUiThread {
                    if (!list.isNullOrEmpty()) {
                        val limited = if (list.size > 6) list.take(6) else list

                        val needFetch = limited.filter { it.mainImageUrl.isNullOrBlank() }
                        if (needFetch.isEmpty()) {
                            productAdapter.setList(limited)
                        } else {
                            val mutable = limited.toMutableList()
                            var remaining = needFetch.size
                            needFetch.forEach { rec ->
                                mainRepository.getProductById(token, rec.productId.toString()) { full ->
                                    activity?.runOnUiThread {
                                        if (full != null) {
                                            val idx = mutable.indexOfFirst { it.productId == rec.productId }
                                            if (idx >= 0) mutable[idx] = full
                                        }
                                        remaining -= 1
                                        if (remaining <= 0) {
                                            productAdapter.setList(mutable)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        mainRepository.getProducts(token) { products ->
                            activity?.runOnUiThread {
                                if (products != null) productAdapter.setList(products.take(6))
                            }
                        }
                    }
                }
            }

            if (!userId.isNullOrBlank()) {
                recommendRepository.getRecommendations(requireContext(), userId, 6, loadDataCallback)
            } else {
                mainRepository.getProducts(token) { products ->
                    activity?.runOnUiThread {
                        if (products != null) productAdapter.setList(products.take(6))
                    }
                }
            }
        }
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }

    override fun initData() {
    }
}