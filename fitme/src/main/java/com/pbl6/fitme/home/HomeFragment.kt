package com.pbl6.fitme.home

import com.pbl6.fitme.model.Category
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
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
import android.widget.SearchView
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeMainViewModel>() {
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
        setupRecyclerViews()
    }
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var allProducts: List<Product> = emptyList()
    private lateinit var productAdapter: ProductAdapter

    private fun setupRecyclerViews() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {
            // --- 1. Lấy và hiển thị DANH MỤC (đã có) ---
            mainRepository.getCategories(token) { categories: List<Category>? ->
                activity?.runOnUiThread {
                    if (categories != null) {
                        Log.d("HomeFragment", "API Products Response: $categories")
                        binding.rvCategories.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                        binding.rvCategories.adapter =
                            CategoryAdapter(categories) { /* no-op selection */ }
                    } else {
                        Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            productAdapter = ProductAdapter()
            binding.rvItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvItems.adapter = productAdapter
            productAdapter.setOnClickItemRecyclerView { product, _ ->
                val bundle = android.os.Bundle().apply {
                    putString("productId", product.productId.toString())
                }
                navigate(R.id.productDetailFragment, bundle)
            }
            mainRepository.getProducts(token) { products: List<com.pbl6.fitme.model.Product>? ->
                activity?.runOnUiThread {
                    if (products != null) {
                        Log.d("HomeFragment", "API Products Response: $products")
                        allProducts = products
                        productAdapter.setList(products)
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
    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }
        // SearchView: filter products as user types
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText ?: "")
                return true
            }
        })
        binding.ivCamera.singleClick {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }
        binding.ivFilter.singleClick {
            // show a popup menu with sorting options
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
        binding.tvAllItems.singleClick {
        }
        // ===== Toolbar click =====
        requireActivity().findViewById<View>(R.id.home_id).singleClick {
            highlightSelectedTab(R.id.home_id)
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
            navigate(R.id.profileFragment)
        }
    }

    override fun initData() { }

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
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

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
    // ===== Dummy Data =====

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
