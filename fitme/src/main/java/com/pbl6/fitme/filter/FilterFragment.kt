package com.pbl6.fitme.filter

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentFilterBinding
import com.pbl6.fitme.home.HomeMainViewModel
import Category
import android.util.Log
import com.pbl6.fitme.profile.CategoryAdapter
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class FilterFragment : BaseFragment<FragmentFilterBinding, HomeMainViewModel>() {

    private val categoryAdapter = CategoryAdapter(emptyList())
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var sizeAdapter: SizeAdapter

    override fun initView() {
        // Hiện toolbar trong Activity
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab person trong toolbar
        highlightSelectedTab(R.id.filter_id)

        setupCategoryRecycler()
        setupColorRecycler()
        setupSizeRecycler()
        setupPriceRange()
        setupButtons()
        // preserve original behavior (was hiding toolbar after setup)
        // hideToolbar()
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
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

    override fun initData() {
        // no-op for now; data is loaded by repository callbacks in setupCategoryRecycler
    }

    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

    private fun setupCategoryRecycler() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            // --- 1. Lấy và hiển thị DANH MỤC (đã có) ---
            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        binding.rvCategory.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(
                                requireContext(),
                                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                                false
                            )
                        // reuse shared adapter
                        categoryAdapter.updateData(categories)
                        binding.rvCategory.adapter = categoryAdapter
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Không lấy được danh mục",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupColorRecycler() {
        val colors = listOf(
            "#FFFFFF", "#000000", "#FF0000", "#00BCD4", "#FFEB3B", "#9C27B0"
        )
        colorAdapter = ColorAdapter(colors)
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }
    }
    private fun setupSizeRecycler() {
        val sizes = listOf("XS", "S", "M", "L", "XL", "2XL")

        sizeAdapter = SizeAdapter(sizes) { selectedSize ->
            //Toast.makeText(requireContext(), "Chọn size: $selectedSize", Toast.LENGTH_SHORT).show()
        }

        binding.rvSizes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSizes.adapter = sizeAdapter
    }

    private fun setupPriceRange() {
        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            binding.tvPrice.text = "$${values[0].toInt()} — $${values[1].toInt()}"
        }
        val greenColor = ColorStateList.valueOf(Color.parseColor("#004CFF")) // Màu main
        binding.priceRangeSlider.trackActiveTintList = greenColor
        binding.priceRangeSlider.thumbTintList = greenColor
        binding.priceRangeSlider.haloTintList = greenColor    }

    private fun setupButtons() {
        binding.btnClear.setOnClickListener {
            // reset filter
            categoryAdapter.clearSelection()
            colorAdapter.clearSelection()
            sizeAdapter.clearSelection()
            binding.radioGroupSort.clearCheck()
            binding.radioGroupPrice.clearCheck()
            binding.priceRangeSlider.values = listOf(10f, 150f)

            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnApply.setOnClickListener {
            val selectedCategory = categoryAdapter.getSelectedCategory()
            val selectedColor = colorAdapter.getSelectedColor()
            val selectedSize = sizeAdapter.getSelectedSize()
            val priceRange = binding.priceRangeSlider.values
            Log.d("FilterFragment", "Applied filters\nCategory: $selectedCategory\nColor: $selectedColor\nSize: $selectedSize\nPrice: ${priceRange[0]} - ${priceRange[1]}")
            Toast.makeText(
                requireContext(),
                "Applied filters\nCategory: $selectedCategory\nColor: $selectedColor\nSize: $selectedSize\nPrice: ${priceRange[0]} - ${priceRange[1]}",
                Toast.LENGTH_SHORT
            ).show()


        }


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
    // BaseFragment will clear binding in onDetach; no need to override onDestroyView here
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
