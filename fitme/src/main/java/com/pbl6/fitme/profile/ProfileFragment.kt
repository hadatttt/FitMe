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
import com.pbl6.fitme.repository.MainRepository
import com.pbl6.fitme.repository.RecommendRepository
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
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
    private val userRepository = UserRepository() // Thêm repo này để lấy info user

    private lateinit var productAdapter: ProductAdapter

    override fun initView() {
        // Setup Toolbar và Tab
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.person_id)

        // Setup RecyclerView sản phẩm
        setupRecyclerViews()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại dữ liệu mỗi khi màn hình hiện lên (quay lại từ Settings hoặc Cart)
        updateOrderBadges()
        fetchUserProfile()
    }

    override fun initListener() {
        // Nút Back
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // Nút Cart kéo thả
        binding.flCart.setDraggableWithClick {
            navigate(R.id.cartFragment)
        }

        // Nút Setting -> Chuyển sang màn hình cài đặt
        binding.btnSetting.singleClick {
            navigate(R.id.settingsFragment)
        }
        binding.detail.singleClick {
            navigateToOrder("pending")
        }

        // Logic Bottom Navigation (Giữ nguyên)
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

    // --- 1. LOGIC LẤY THÔNG TIN USER (MỚI) ---
    // --- 1. LOGIC LẤY THÔNG TIN USER ---
    private fun fetchUserProfile() {
        val session = SessionManager.getInstance()
        // Lấy token ở đây để truyền vào Glide
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
                            // Xử lý đường dẫn URL
                            val baseUrl = "http://10.48.170.90:8080/api" // Đảm bảo IP đúng
                            val fullPath = if (url.startsWith("/")) url else "/$url"
                            val stringUrl = "$baseUrl$fullPath"

                            // --- KHẮC PHỤC LỖI MISSING HEADER TẠI ĐÂY ---
                            // Tạo GlideUrl có chứa Header Authorization
                            val glideUrl = GlideUrl(
                                stringUrl,
                                LazyHeaders.Builder()
                                    .addHeader("Authorization", "Bearer $token")
                                    .build()
                            )

                            // Load ảnh dùng glideUrl thay vì stringUrl
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

    // --- 2. LOGIC BADGE ĐƠN HÀNG ---
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

                        // Map counts to XML TextViews
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

    // --- 3. CÁC HÀM HỖ TRỢ KHÁC ---
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

            // Logic lấy Recommendation hoặc Top Products
            val loadDataCallback: (List<Product>?) -> Unit = { list ->
                activity?.runOnUiThread {
                    if (!list.isNullOrEmpty()) {
                        val limited = if (list.size > 6) list.take(6) else list

                        // Kiểm tra nếu sản phẩm thiếu ảnh thì gọi API lấy chi tiết
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
                        // Fallback nếu không có recommendation
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
        // Data đã được load trong onResume và initView
    }
}