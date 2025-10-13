package com.pbl6.fitme.profile

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProfileBinding
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.model.Product
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    private val mainRepository = com.pbl6.fitme.repository.MainRepository

    override fun initView() {
        // Hiện toolbar trong Activity
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab person trong toolbar
        highlightSelectedTab(R.id.person_id)

        // Lấy dữ liệu từ API bằng Retrofit
        mainRepository.getCategories { categories: List<Category>? ->
            if (categories != null) {
                android.util.Log.d("ProfileFragment", "Categories loaded: ${categories.size}")
                setupRecyclerViewCategory(binding.rvTopProducts, categories)
                setupRecyclerViewCategory(binding.rvStories, categories)
            } else {
                android.util.Log.e("ProfileFragment", "Failed to load categories or categories is null")
                // Xử lý lỗi hoặc hiển thị thông báo
            }
        }
        mainRepository.getProducts { products: List<Product>? ->
            if (products != null) {
                android.util.Log.d("ProfileFragment", "Products loaded: ${products.size}")
                setupRecyclerViewProduct(binding.rvNewItems, products)
            } else {
                android.util.Log.e("ProfileFragment", "Failed to load products or products is null")
                // Xử lý lỗi hoặc hiển thị thông báo
            }
        }
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

    // ===== Toolbar Helpers =====
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
    private fun setupRecyclerViewCategory(rv: RecyclerView, data: List<Category>) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = CategoryAdapter(data)
    }

    private fun setupRecyclerViewProduct(rv: RecyclerView, data: List<Product>) {
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = ProductAdapter(data)
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
