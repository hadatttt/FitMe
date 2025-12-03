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

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {

    // Person image picker (choose from album) — replaces camera capture
    private val personPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    requireContext().contentResolver.loadThumbnail(uri, android.util.Size(800, 800), null)
                } else {
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                }
                personBitmap = bmp
                binding.ivCamera.setImageBitmap(bmp)
                // after selecting person, prompt to pick a cloth image
                clothPicker.launch("image/*")
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Cannot load selected image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    // pick a cloth image from gallery
    private val clothPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // API 29+ has loadThumbnail which is efficient
                    requireContext().contentResolver.loadThumbnail(uri, android.util.Size(800, 800), null)
                } else {
                    // Fallback for older devices
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                }
                clothBitmap = bmp
                if (personBitmap != null && clothBitmap != null) {
                    performTryOn(personBitmap!!, clothBitmap!!)
                }
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Failed to load cloth image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No cloth selected", Toast.LENGTH_SHORT).show()
        }
    }

    private var personBitmap: Bitmap? = null
    private var clothBitmap: Bitmap? = null
    private val tryOnRepo = com.pbl6.fitme.repository.TryOnRepository()

    private fun performTryOn(person: Bitmap, cloth: Bitmap) {
        binding.progressBar.visibility = View.VISIBLE
        tryOnRepo.virtualTryOn(requireContext(), person, cloth) { bytes ->
            activity?.runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (bytes != null && bytes.isNotEmpty()) {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        // show result in a dialog
                        val dlg = android.app.Dialog(requireContext())
                        val iv = android.widget.ImageView(requireContext())
                        iv.setImageBitmap(bmp)
                        dlg.setContentView(iv)
                        dlg.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                        dlg.show()
                    } catch (ex: Exception) {
                        Toast.makeText(requireContext(), "Failed to render try-on result", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Try-on failed or returned empty image", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // No recommendations in HomeFragment: only run Try-On
    }

    override fun initView() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        highlightSelectedTab(R.id.home_id)
        setupRecyclerViews()
    }

    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var allProducts: List<Product> = emptyList()
    private lateinit var productAdapter: ProductAdapter

    private fun setupRecyclerViews() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            productAdapter = ProductAdapter()
            binding.rvItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvItems.adapter = productAdapter

            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }
            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        Log.d("HomeFragment", "Categories: $categories")
                        binding.rvCategories.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)

                        binding.rvCategories.adapter = CategoryAdapter(categories) { selectedCategory ->
                            val catId = selectedCategory.categoryId
                            if (catId != null) {
                                loadProductsByCategory(token, catId.toString())
                                Toast.makeText(requireContext(), "Đang tải: ${selectedCategory.categoryName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            loadAllProducts(token)

        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem dữ liệu", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAllProducts(token: String) {
        mainRepository.getProducts(token) { products: List<Product>? ->
            activity?.runOnUiThread {
                if (products != null) {
                    Log.d("HomeFragment", "All Products: ${products.size}")
                    allProducts = products
                    productAdapter.setList(products)
                } else {
                    Toast.makeText(requireContext(), "Không lấy được sản phẩm", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- UPDATE: Hàm mới - Load sản phẩm theo Category ID ---
    private fun loadProductsByCategory(token: String, categoryId: String) {
        // Có thể thêm loading bar ở đây nếu muốn
        mainRepository.getProductsByCategory(token, categoryId) { products: List<Product>? ->
            activity?.runOnUiThread {
                if (products != null) {
                    Log.d("HomeFragment", "Category Products: ${products.size}")
                    // Cập nhật biến allProducts để chức năng tìm kiếm (filterProducts) vẫn hoạt động trên danh sách mới này
                    allProducts = products
                    productAdapter.setList(products)

                    // Cuộn lên đầu trang
                    binding.rvItems.scrollToPosition(0)
                } else {
                    // Nếu API trả về null hoặc lỗi -> Xóa danh sách cũ
                    productAdapter.setList(emptyList())
                    Toast.makeText(requireContext(), "Không có sản phẩm nào", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }

        // Search logic
        binding.etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivCamera.singleClick {
            // Open album to select person image (user requested album selection instead of camera)
            personPicker.launch("image/*")
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

        // --- UPDATE: Xử lý click vào nút "All Items" (hoặc tiêu đề danh sách) để reset về load tất cả ---
        binding.tvAllItems.singleClick {
            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (!token.isNullOrBlank()) {
                loadAllProducts(token)
                Toast.makeText(requireContext(), "Hiển thị tất cả sản phẩm", Toast.LENGTH_SHORT).show()
            }
        }

        // ===== Toolbar click logic (Giữ nguyên) =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
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
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() { }

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

    // camera capture removed in favor of album picker

    private fun sortByNameAsc() {
        val sorted = allProducts.sortedBy { it.productName?.lowercase() }
        allProducts = sorted
        productAdapter.setList(sorted)
    }

    private fun sortByPriceAsc() {
        val sorted = allProducts.sortedBy { it.minPrice ?: Double.MAX_VALUE }
        allProducts = sorted
        productAdapter.setList(sorted)
    }

    private fun sortByPriceDesc() {
        val sorted = allProducts.sortedByDescending { it.minPrice ?: Double.MIN_VALUE }
        allProducts = sorted
        productAdapter.setList(sorted)
    }

    private fun filterProducts(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            productAdapter.setList(allProducts)
            return
        }
        val filtered = allProducts.filter { p ->
            val name = try { p.productName ?: "" } catch (e: Exception) { "" }
            name.lowercase().contains(q)
        }
        productAdapter.setList(filtered)
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}