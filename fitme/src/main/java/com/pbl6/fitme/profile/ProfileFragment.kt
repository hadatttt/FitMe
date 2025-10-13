package com.pbl6.fitme.profile

import Category
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProfileBinding
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

    override fun initView() {
        val session = SessionManager.getInstance()
        val token = session.getAccessToken(requireContext())
        Log.d("SessionManager", "AccessToken = $token")
        // Hiện toolbar trong Activity
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab person trong toolbar
        highlightSelectedTab(R.id.person_id)

        setupRecyclerViews()
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }
        binding.flCart.setDraggableWithClick {
            navigate(R.id.cartFragment)
        }
        // ===== Button trong Profile =====
        binding.btnMyActivity.singleClick {
        }
        binding.btnSetting.singleClick {
            navigate(R.id.settingsFragment)
        }
        binding.btnNotification.singleClick {
        }
        binding.btnVoucher.singleClick {
        }
        binding.ivSeeAllNewItems.singleClick {
        }
        binding.ivSeeAllNotification.singleClick {
        }

        // ===== Toolbar click =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
            navigate(R.id.homeFragment)
        }
        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
            highlightSelectedTab(R.id.wish_id)
            navigate(R.id.wishlistFragment)
        }
        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
            highlightSelectedTab(R.id.filter_id)
            navigate(R.id.filterFragment)
        }
        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
            highlightSelectedTab(R.id.cart_id)
            navigate(R.id.cartFragment)
        }
        requireActivity().findViewById<View>(R.id.person_id).singleClick {
            highlightSelectedTab(R.id.person_id)

        }
    }

    override fun initData() {
    }
    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
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

    // ===== RecyclerView Helpers =====
    private fun setupRecyclerViews() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            // --- 1. Lấy và hiển thị DANH MỤC (đã có) ---
            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        binding.rvStories.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                        binding.rvStories.adapter =
                            CategoryAdapter(categories) { /* no-op selection */ }
                    } else {
                        Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // --- 2. Lấy và hiển thị SẢN PHẨM (phần thêm mới) ---
            mainRepository.getProducts(token) { products: List<com.pbl6.fitme.model.Product>? ->
                activity?.runOnUiThread {
                    if (products != null) {
                        // Giả sử RecyclerView cho sản phẩm có id là rvItems
                        binding.rvTopProducts.layoutManager =
                            androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2) // Hiển thị dạng lưới 2 cột
                        binding.rvTopProducts.adapter =
                            ProductAdapter(products)
                    } else {
                        Toast.makeText(requireContext(), "Không lấy được sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } else {
            // Gộp chung thông báo khi chưa đăng nhập
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem dữ liệu", Toast.LENGTH_LONG).show()
        }
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
