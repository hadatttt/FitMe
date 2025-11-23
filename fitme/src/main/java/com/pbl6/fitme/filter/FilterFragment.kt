//package com.pbl6.fitme.filter
//
//import android.content.res.ColorStateList
//import android.graphics.Color
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.pbl6.fitme.R
//import com.pbl6.fitme.databinding.FragmentFilterBinding
//import com.pbl6.fitme.home.HomeMainViewModel
//import com.pbl6.fitme.model.Category
//import android.util.Log
//import com.pbl6.fitme.profile.CategoryAdapter
//import hoang.dqm.codebase.base.activity.BaseFragment
//import hoang.dqm.codebase.base.activity.navigate
//import hoang.dqm.codebase.base.activity.onBackPressed
//import hoang.dqm.codebase.base.activity.popBackStack
//import hoang.dqm.codebase.utils.singleClick
//
//class FilterFragment : BaseFragment<FragmentFilterBinding, HomeMainViewModel>() {
//
//    private val categoryAdapter = CategoryAdapter(emptyList())
//    private lateinit var colorAdapter: ColorAdapter
//    private lateinit var sizeAdapter: SizeAdapter
//
//    override fun initView() {
//        // Hiện toolbar trong Activity
//        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
//        toolbar.visibility = View.VISIBLE
//
//        // Highlight tab person trong toolbar
//        highlightSelectedTab(R.id.filter_id)
//
//    setupCategoryRecycler()
//    setupColorRecycler()
//    setupSizeRecycler()
//    setupPriceRange()
//    setupButtons()
//    setupGenderSeason()
//        // preserve original behavior (was hiding toolbar after setup)
//        // hideToolbar()
//    }
//
//    override fun initListener() {
//        onBackPressed {
//            hideToolbar()
//            popBackStack()
//        }
//        // ===== Toolbar click =====
//        requireActivity().findViewById<View>(R.id.home_id).singleClick {
//            highlightSelectedTab(R.id.home_id)
//            navigate(R.id.homeFragment)
//        }
//        requireActivity().findViewById<View>(R.id.wish_id).singleClick {
//            highlightSelectedTab(R.id.wish_id)
//            navigate(R.id.wishlistFragment)
//        }
//        requireActivity().findViewById<View>(R.id.filter_id).singleClick {
//            highlightSelectedTab(R.id.filter_id)
//        }
//        requireActivity().findViewById<View>(R.id.cart_id).singleClick {
//            highlightSelectedTab(R.id.cart_id)
//            navigate(R.id.cartFragment)
//        }
//        requireActivity().findViewById<View>(R.id.person_id).singleClick {
//            highlightSelectedTab(R.id.person_id)
//            navigate(R.id.profileFragment)
//        }
//    }
//
//    override fun initData() {
//        // no-op for now; data is loaded by repository callbacks in setupCategoryRecycler
//    }
//
//    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
//
//    private fun setupCategoryRecycler() {
//        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
//
//        if (!token.isNullOrBlank()) {
//            // --- 1. Lấy và hiển thị DANH MỤC (đã có) ---
//            mainRepository.getCategories(token) { categories: List<Category>? ->
//                activity?.runOnUiThread {
//                    if (categories != null) {
//                        binding.rvCategory.layoutManager =
//                            androidx.recyclerview.widget.LinearLayoutManager(
//                                requireContext(),
//                                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
//                                false
//                            )
//                        // reuse shared adapter
//                        categoryAdapter.updateData(categories)
//                        binding.rvCategory.adapter = categoryAdapter
//                    } else {
//                        Toast.makeText(
//                            requireContext(),
//                            "Không lấy được danh mục",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//        }
//    }
//
//    private fun setupColorRecycler() {
//        val colors = listOf(
//            "#FFFFFF", "#000000", "#FF0000", "#00BCD4", "#FFEB3B", "#9C27B0"
//        )
//        colorAdapter = ColorAdapter(colors)
//        binding.rvColors.apply {
//            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            adapter = colorAdapter
//        }
//    }
//    private fun setupSizeRecycler() {
//        val sizes = listOf("XS", "S", "M", "L", "XL", "2XL")
//
//        sizeAdapter = SizeAdapter(sizes) { selectedSize ->
//            //Toast.makeText(requireContext(), "Chọn size: $selectedSize", Toast.LENGTH_SHORT).show()
//        }
//
//        binding.rvSizes.layoutManager =
//            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//        binding.rvSizes.adapter = sizeAdapter
//    }
//
//    private fun setupGenderSeason() {
//        // Gender spinner
//        val genders = listOf("Any", "men", "women", "unisex")
//        val genderAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genders)
//        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spinnerGender.adapter = genderAdapter
//
//        // Season spinner
//        val seasons = listOf("Any", "summer", "winter", "all_season")
//        val seasonAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasons)
//        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spinnerSeason.adapter = seasonAdapter
//    }
//
//    private fun setupPriceRange() {
//        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
//            val values = slider.values
//            binding.tvPrice.text = "$${values[0].toInt()} — $${values[1].toInt()}"
//        }
//        val greenColor = ColorStateList.valueOf(Color.parseColor("#004CFF")) // Màu main
//        binding.priceRangeSlider.trackActiveTintList = greenColor
//        binding.priceRangeSlider.thumbTintList = greenColor
//        binding.priceRangeSlider.haloTintList = greenColor    }
//
//    private fun setupButtons() {
//        binding.btnClear.setOnClickListener {
//            // reset filter
//            categoryAdapter.clearSelection()
//            colorAdapter.clearSelection()
//            sizeAdapter.clearSelection()
//            binding.radioGroupSort.clearCheck()
//            binding.radioGroupPrice.clearCheck()
//            binding.priceRangeSlider.values = listOf(10f, 150f)
//
//            Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show()
//            // Clear any displayed results as well
////            binding.rvItems.adapter = com.pbl6.fitme.profile.ProductAdapter(emptyList())
//            binding.rvItems.visibility = View.GONE
//        }
//
//        binding.btnApply.setOnClickListener {
//            val selectedCategory = categoryAdapter.getSelectedCategory()
//            val selectedColorHex = colorAdapter.getSelectedColor()
//            val selectedColor = hexToColorName(selectedColorHex)?.trim()?.lowercase()
//            val selectedSize = sizeAdapter.getSelectedSize()?.trim()?.uppercase()
//            val selectedGender = binding.spinnerGender.selectedItem?.toString()?.let { if (it == "Any") null else it.trim().lowercase() }
//            val selectedSeason = binding.spinnerSeason.selectedItem?.toString()?.let { if (it == "Any") null else it.trim().lowercase() }
//            val priceRange = binding.priceRangeSlider.values
//            Log.d("FilterFragment", "Applied filters\nCategory: $selectedCategory\nColor: $selectedColor (from $selectedColorHex)\nSize: $selectedSize\nPrice: ${priceRange[0]} - ${priceRange[1]}")
//            Toast.makeText(
//                requireContext(),
//                "Applied filters\nCategory: $selectedCategory\nColor: $selectedColor\nSize: $selectedSize\nPrice: ${priceRange[0]} - ${priceRange[1]}",
//                Toast.LENGTH_SHORT
//            ).show()
//
//            // Apply filters: fetch products from repository and filter client-side
//            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
//            if (token.isNullOrBlank()) {
//                Toast.makeText(requireContext(), "Vui lòng đăng nhập để lọc sản phẩm", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            mainRepository.getProducts(token) { products ->
//                activity?.runOnUiThread {
//                    if (products == null) {
//                        Toast.makeText(requireContext(), "Không lấy được sản phẩm để lọc", Toast.LENGTH_SHORT).show()
//                        return@runOnUiThread
//                    }
//
//                    // priceRange values are floats like [min, max]
//                    val minPrice = priceRange[0].toDouble()
//                    val maxPrice = priceRange[1].toDouble()
//
//                    val filtered = products.filter { p ->
//                        // Category filter
//                        val okCategory = selectedCategory == null || p.categoryName.equals(selectedCategory, ignoreCase = true)
//
//                        // Gender filter (assume product.gender exists in model - fallback to true if not present)
////                        val productGender = try { p.gender?.trim()?.lowercase() } catch (_: Exception) { null }
////                        val okGender = selectedGender == null || (productGender != null && productGender == selectedGender)
////
////                        // Season filter (assume product.season exists in model)
////                        val productSeason = try { p.season?.trim()?.lowercase() } catch (_: Exception) { null }
////                        val okSeason = selectedSeason == null || (productSeason != null && productSeason == selectedSeason)
//
//                        // Price filter: at least one variant in price range
//                        val okPrice = p.variants.any { v -> v.price >= minPrice && v.price <= maxPrice }
//
//                        // Color/Size filter: if both color and size selected, require same variant matching both.
//                        val okColorSize = when {
//                            selectedColor == null && selectedSize == null -> true
//                            selectedColor != null && selectedSize == null -> p.variants.any { v -> (v.color ?: "").trim().lowercase() == selectedColor }
//                            selectedColor == null && selectedSize != null -> p.variants.any { v -> (v.size ?: "").trim().uppercase() == selectedSize }
//                            else -> p.variants.any { v -> (v.color ?: "").trim().lowercase() == selectedColor && (v.size ?: "").trim().uppercase() == selectedSize }
//                        }
//
//                        okCategory && okPrice && okColorSize
//                    }
//
//                    // Show results in the fragment's RecyclerView (reuse rvItems)
//                    if (filtered.isEmpty()) {
//                        Toast.makeText(requireContext(), "No products match filters", Toast.LENGTH_SHORT).show()
////                        // show empty adapter
////                        binding.rvItems.adapter = com.pbl6.fitme.profile.ProductAdapter(emptyList())
//                        binding.rvItems.visibility = View.GONE
//                    } else {
//                        // Apply sorting
//                        val sorted = run {
//                            var list = filtered.toMutableList()
//                            // Sort by Popular / Newest
//                            val rbPopular = binding.rbPopular.isChecked
//                            val rbNewest = binding.rbNewest.isChecked
//                            if (rbPopular) {
//                                // popular: sort by review count desc
//                                list.sortByDescending { it.reviews?.size ?: 0 }
//                            } else if (rbNewest) {
//                                // newest: sort by createdAt desc (ISO timestamp assumed)
//                                list.sortWith(compareByDescending<com.pbl6.fitme.model.Product> { p -> p.createdAt ?: "" })
//                            }
//
//                            // Price radio group
//                            val priceHighToLow = binding.rbPriceHighLow.isChecked
//                            val priceLowToHigh = binding.rbPriceLowHigh.isChecked
//                            if (priceHighToLow) {
//                                list.sortByDescending { prod -> prod.variants.minOfOrNull { it.price } ?: Double.MAX_VALUE }
//                            } else if (priceLowToHigh) {
//                                list.sortBy { prod -> prod.variants.minOfOrNull { it.price } ?: Double.MAX_VALUE }
//                            }
//
//                            list
//                        }
//
//                        binding.rvItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
//                        // Create and populate the product adapter so results are displayed
//                        val productAdapter = com.pbl6.fitme.profile.ProductAdapter()
//                        productAdapter.setOnClickItemRecyclerView { product, _ ->
//                            val bundle = android.os.Bundle().apply {
//                                putString("productId", product.productId.toString())
//                            }
//                            navigate(R.id.productDetailFragment, bundle)
//                        }
//                        productAdapter.setList(sorted)
//                        binding.rvItems.adapter = productAdapter
//                        binding.rvItems.visibility = View.VISIBLE
//                    }
//                }
//            }
//        }
//    }
//    // ===== Toolbar Helpers =====
//    private fun highlightSelectedTab(selectedId: Int) {
//        val ids = listOf(R.id.home_id, R.id.wish_id, R.id.filter_id, R.id.cart_id, R.id.person_id)
//        ids.forEach { id ->
//            val view = requireActivity().findViewById<View>(id)
//            if (id == selectedId) {
//                view.setBackgroundResource(R.drawable.bg_selected_tab)
//            } else {
//                view.setBackgroundColor(
//                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
//                )
//            }
//        }
//    }
//    // BaseFragment will clear binding in onDetach; no need to override onDestroyView here
//    private fun hideToolbar() {
//        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
//        toolbar.visibility = View.GONE
//    }
//
//    // Map common hex color codes to friendly color names for sending to BE / filtering
//    private fun hexToColorName(hex: String?): String? {
//        if (hex.isNullOrBlank()) return null
//        return when (hex.trim().uppercase()) {
//            "#000000", "000000" -> "Black"
//            "#FFFFFF", "FFFFFF" -> "White"
//            "#FF0000", "FF0000" -> "Red"
//            "#00BCD4", "00BCD4" -> "Cyan"
//            "#FFEB3B", "FFEB3B" -> "Yellow"
//            "#9C27B0", "9C27B0" -> "Purple"
//            else -> {
//                // Basic heuristic: if hex looks like #RRGGBB, try to map to nearest common name or return the hex
//                if (hex.startsWith("#")) {
//                    // fallback: return hex as-is but normalized
//                    hex.uppercase()
//                } else hex
//            }
//        }
//    }
//}
