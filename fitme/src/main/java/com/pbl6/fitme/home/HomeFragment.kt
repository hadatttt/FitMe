package com.pbl6.fitme.home

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.checkin.CheckInDialogFragment
import com.pbl6.fitme.databinding.FragmentHomeBinding
import com.pbl6.fitme.profile.CategoryAdapter
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.session.SessionManager
import com.pbl6.fitme.untils.AppSharePref
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.setDraggableWithClick
import hoang.dqm.codebase.utils.singleClick

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {

    private lateinit var productAdapter: ProductAdapter
    private var isProductMode = false // Biến theo dõi trạng thái hiện tại

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.home_id)

        // Mặc định hiển thị Category Mode
        toggleViewMode(showProducts = false)

        setupRecyclerViews()
        observeViewModel()

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_LONG).show()
        } else {
            viewModel.fetchData(token)
        }
    }

    override fun initData() {
        val pref = AppSharePref(requireContext())
        if (!pref.isTodaySaved(requireContext())) {
            val dialog = CheckInDialogFragment()
            dialog.show(parentFragmentManager, "CheckInDialog")
            pref.saveToday(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.obsCategories.observe(this) { categories ->
            // Khi load xong category, hiển thị vào Grid
            binding.rvCategories.adapter = CategoryAdapter(categories) { selectedCategory ->
                // CLICK VÀO CATEGORY
                val catId = selectedCategory.categoryId
                val token = SessionManager.getInstance().getAccessToken(requireContext())

                binding.tvAllItems.text = selectedCategory.categoryName ?: "Collection"

                if (catId != null && !token.isNullOrBlank()) {
                    viewModel.loadProductsByCategory(token, catId.toString())
                    // Chuyển sang chế độ xem sản phẩm
                    toggleViewMode(showProducts = true)
                }
            }
        }

        viewModel.obsProducts.observe(this) { products ->
            productAdapter.setList(products)
            binding.rvItems.scrollToPosition(0)
        }

        viewModel.obsCurrentGender.observe(this) { gender ->
            updateGenderTabUI(gender)
            // Khi đổi giới tính, nên reset về màn hình Category để chọn lại
            toggleViewMode(showProducts = false)
        }
    }

    override fun initListener() {
        // Xử lý nút Back cứng của điện thoại
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isProductMode) {
                    // Nếu đang xem sản phẩm -> Quay lại xem danh mục
                    toggleViewMode(showProducts = false)
                } else {
                    // Nếu đang ở danh mục -> Thoát/Ẩn toolbar như cũ
                    isEnabled = false
                    hideToolbar()
                    popBackStack()
                }
            }
        })

        // Nút tắt (X) ở màn hình sản phẩm
        binding.btnCloseProductMode.singleClick {
            toggleViewMode(showProducts = false)
        }

        binding.btnSlot.setDraggableWithClick {
            navigate(R.id.slotMachineGameFragment)
        }

        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: ""

        binding.tvTabWoman.setOnClickListener { viewModel.changeGenderFilter("WOMAN", token) }
        binding.tvTabMan.setOnClickListener { viewModel.changeGenderFilter("MAN", token) }
        binding.tvTabKid.setOnClickListener { viewModel.changeGenderFilter("KID", token) }

        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Khi search, có thể muốn hiện list sản phẩm luôn
                if (!s.isNullOrEmpty() && !isProductMode) {
                    toggleViewMode(showProducts = true)
                    binding.tvAllItems.text = "Search Results"
                }
                viewModel.searchProduct(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivFilter.singleClick {
            showSortPopupMenu()
        }

        setupBottomNavigation()
    }

    // Hàm chuyển đổi giao diện
    private fun toggleViewMode(showProducts: Boolean) {
        isProductMode = showProducts
        if (showProducts) {
            binding.containerCategoryMode.visibility = View.GONE
            binding.containerProductMode.visibility = View.VISIBLE
        } else {
            binding.containerCategoryMode.visibility = View.VISIBLE
            binding.containerProductMode.visibility = View.GONE
            // Reset lại title nếu cần
            binding.tvAllItems.text = "Collections"
        }
    }

    private fun setupRecyclerViews() {
        if (!::productAdapter.isInitialized) {
            productAdapter = ProductAdapter()
            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }
        }

        binding.rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvItems.adapter = productAdapter

        // SỬA: Category hiển thị dạng Grid 2 cột thay vì Horizontal
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    // ... (Giữ nguyên các hàm updateGenderTabUI, highlightSelectedTab, setupBottomNavigation, showSortPopupMenu, hideToolbar cũ) ...

    private fun updateGenderTabUI(selectedGender: String) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.black)
        val unselectedColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        fun updateStyle(tv: TextView, isSelected: Boolean) {
            tv.setTextColor(if (isSelected) selectedColor else unselectedColor)
            tv.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        updateStyle(binding.tvTabWoman, selectedGender == "WOMAN")
        updateStyle(binding.tvTabMan, selectedGender == "MAN")
        updateStyle(binding.tvTabKid, selectedGender == "KID")
    }

    private fun highlightSelectedTab(selectedId: Int) {
        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.cart_id, R.id.person_id)
        ids.forEach { id ->
            val view = requireActivity().findViewById<View>(id)
            if (id == selectedId) {
                view.setBackgroundResource(R.drawable.bg_selected_tab)
            } else {
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            }
        }
    }

    private fun setupBottomNavigation() {
        requireActivity().findViewById<View>(R.id.home_id).singleClick { highlightSelectedTab(R.id.home_id) }
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
            navigate(R.id.profileFragment)
        }
    }

    private fun showSortPopupMenu() {
        val popup = PopupMenu(requireContext(), binding.ivFilter)
        popup.menu.add(0, 1, 0, "Name A -> Z")
        popup.menu.add(0, 2, 1, "Price low -> high")
        popup.menu.add(0, 3, 2, "Price high -> low")
        popup.setOnMenuItemClickListener { menuItem ->
            viewModel.sortProducts(menuItem.itemId)
            true
        }
        popup.show()
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}