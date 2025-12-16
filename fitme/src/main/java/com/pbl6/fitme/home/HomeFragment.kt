package com.pbl6.fitme.home

import android.text.TextWatcher
import com.pbl6.fitme.model.Category
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentHomeBinding
import com.pbl6.fitme.profile.CategoryAdapter
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.model.Product
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import java.util.Locale

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {

    // --- Variables for Data Caching ---
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var allProducts: List<Product> = emptyList() // Lưu danh sách gốc tất cả sản phẩm
    private var allCategories: List<Category> = emptyList() // Lưu danh sách gốc tất cả danh mục
    private var currentGenderFilter: String = "WOMAN" // Mặc định là WOMAN

    private lateinit var productAdapter: ProductAdapter

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    binding.ivCamera.setImageBitmap(photo)
                } else {
                    Toast.makeText(requireContext(), "Cannot capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.home_id)

        // Cập nhật giao diện Tab mặc định
        updateGenderTabUI(currentGenderFilter)

        setupRecyclerViews()
    }

    private fun setupRecyclerViews() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            // 1. Setup Adapter cho Sản phẩm
            productAdapter = ProductAdapter()
            binding.rvItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvItems.adapter = productAdapter

            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }

            // 2. Tải Danh mục (Category)
            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        Log.d("HomeFragment", "Categories loaded: ${categories.size}")
                        allCategories = categories // Lưu lại danh sách gốc

                        // Setup LayoutManager cho Category
                        binding.rvCategories.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)

                        // Lọc và hiển thị Category theo Tab hiện tại (WOMAN)
                        filterCategoriesAndDisplay(currentGenderFilter, token)
                    } else {
                        Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 3. Tải tất cả Sản phẩm
            loadAllProducts(token)

        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem dữ liệu", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAllProducts(token: String) {
        mainRepository.getProducts(token) { products: List<Product>? ->
            activity?.runOnUiThread {
                if (products != null) {
                    Log.d("HomeFragment", "All Products loaded: ${products.size}")
                    allProducts = products
                    // Hiển thị sản phẩm lọc theo Tab hiện tại luôn
                    filterProductsByGender(currentGenderFilter)
                } else {
                    Toast.makeText(requireContext(), "Không lấy được sản phẩm", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProductsByCategory(token: String, categoryId: String) {
        mainRepository.getProductsByCategory(token, categoryId) { products: List<Product>? ->
            activity?.runOnUiThread {
                if (products != null) {
                    // Khi click vào danh mục cụ thể, chỉ hiển thị sản phẩm của danh mục đó
                    productAdapter.setList(products)
                    binding.rvItems.scrollToPosition(0)
                } else {
                    productAdapter.setList(emptyList())
                    Toast.makeText(requireContext(), "Không có sản phẩm nào", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- LOGIC LỌC (QUAN TRỌNG) ---

    // 1. Lọc và hiển thị Categories
    private fun filterCategoriesAndDisplay(gender: String, token: String) {
        val filteredCats = allCategories.filter { cat ->
            val name = cat.categoryName?.lowercase(Locale.ROOT) ?: ""
            // Logic lọc theo từ khóa
            when (gender) {
                "WOMAN" -> name.contains("women") || name.contains("woman") || name.contains("lady") || name.contains("girl") || name.contains("dress") || name.contains("skirt")
                "MAN" -> name.contains("men") || name.contains("man") || name.contains("boy") || name.contains("gentle") || name.contains("shirt")
                "KID" -> name.contains("kids's") || name.contains("baby") || name.contains("child")
                else -> true
            }
        }

        // Cập nhật Adapter Category
        binding.rvCategories.adapter = CategoryAdapter(filteredCats) { selectedCategory ->
            val catId = selectedCategory.categoryId
            if (catId != null) {
                loadProductsByCategory(token, catId.toString())
                Toast.makeText(requireContext(), "Đang tải: ${selectedCategory.categoryName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. Lọc và hiển thị Products (cho list New Arrivals)
    private fun filterProductsByGender(gender: String) {
        val filteredProducts = allProducts.filter { p ->
            val name = p.productName?.lowercase(Locale.ROOT) ?: ""
            // Kiểm tra tên sản phẩm
            when (gender) {
                // Với Woman: lấy những cái có chữ nữ, váy... VÀ loại bỏ những cái có chữ Men/Man (để tránh nhầm lẫn nếu tên là "Woman looking at Man shirt")
                "WOMAN" -> !name.contains("men") && !name.contains("man") &&
                        (name.contains("women") || name.contains("woman") || name.contains("lady") || name.contains("girl") || name.contains("dress") || name.contains("skirt"))
                "MAN" -> name.contains("men") || name.contains("man") || name.contains("boy")
                "KID" -> name.contains("kid") || name.contains("baby")
                else -> true
            }
        }

        // Nếu lọc xong mà rỗng (do tên sản phẩm đặt không chuẩn), có thể hiển thị allProducts hoặc để trống.
        // Ở đây mình để trống để đúng logic lọc.
        productAdapter.setList(filteredProducts)
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // --- Bắt sự kiện Click cho 3 Tab ---
        binding.tvTabWoman.setOnClickListener { onGenderTabSelected("WOMAN") }
        binding.tvTabMan.setOnClickListener { onGenderTabSelected("MAN") }
        binding.tvTabKid.setOnClickListener { onGenderTabSelected("KID") }

        // Search logic
        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivCamera.singleClick {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }

        // Filter / Sort popup
        binding.ivFilter.singleClick {
            val popup = android.widget.PopupMenu(requireContext(), binding.ivFilter)
            popup.menu.add(0, 1, 0, "Name A -> Z")
            popup.menu.add(0, 2, 1, "Price low -> high")
            popup.menu.add(0, 3, 2, "Price high -> low")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> sortByNameAsc()
                    2 -> sortByPriceAsc()
                    3 -> sortByPriceDesc()
                }
                true
            }
            popup.show()
        }

        // Click "All Items" -> Reset về list của Gender hiện tại
        binding.tvAllItems.singleClick {
            filterProductsByGender(currentGenderFilter)
            Toast.makeText(requireContext(), "All $currentGenderFilter Items", Toast.LENGTH_SHORT).show()
        }

        // Toolbar logic
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

    override fun initData() {

    }

    // Xử lý khi chọn Tab
    private fun onGenderTabSelected(gender: String) {
        if (currentGenderFilter == gender) return // Đang chọn rồi thì thôi

        currentGenderFilter = gender
        updateGenderTabUI(gender) // Đổi màu chữ

        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext()) ?: ""

        // 1. Lọc lại Category
        filterCategoriesAndDisplay(gender, token)

        // 2. Lọc lại List sản phẩm bên dưới
        filterProductsByGender(gender)
    }

    // Đổi màu chữ đậm/nhạt cho Tab
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
                view.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
            }
        }
    }

    // Sort functions (Sort trên toàn bộ list rồi lọc lại theo gender để đảm bảo đúng view)
    private fun sortByNameAsc() {
        allProducts = allProducts.sortedBy { it.productName?.lowercase() }
        filterProductsByGender(currentGenderFilter)
    }

    private fun sortByPriceAsc() {
        allProducts = allProducts.sortedBy { it.minPrice ?: Double.MAX_VALUE }
        filterProductsByGender(currentGenderFilter)
    }

    private fun sortByPriceDesc() {
        allProducts = allProducts.sortedByDescending { it.minPrice ?: Double.MIN_VALUE }
        filterProductsByGender(currentGenderFilter)
    }

    private fun filterProducts(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            filterProductsByGender(currentGenderFilter)
            return
        }
        // Khi search text thì search trên toàn bộ (hoặc có thể search trong gender hiện tại tùy logic, ở đây search all)
        val filtered = allProducts.filter { p ->
            val name = try { p.productName ?: "" } catch (e: Exception) { "" }
            name.lowercase().contains(q)
        }
        productAdapter.setList(filtered)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}