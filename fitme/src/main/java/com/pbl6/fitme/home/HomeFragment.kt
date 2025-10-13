package com.pbl6.fitme.home

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.profile.*
import com.pbl6.fitme.databinding.FragmentHomeBinding
import com.pbl6.fitme.model.Category
import com.pbl6.fitme.model.Product
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
        // Hiện toolbar trong Activity
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE

        // Highlight tab person trong toolbar
        highlightSelectedTab(R.id.home_id)

        // Setup RecyclerView
        setupRecyclerViews()
    }

    private val mainRepository = com.pbl6.fitme.repository.MainRepository

    private fun setupRecyclerViews() {
        mainRepository.getCategories { categories: List<Category>? ->
            if (categories != null) {
                binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
                binding.rvCategories.adapter = CategoryAdapter(categories)
            } else {
                Toast.makeText(requireContext(), "Không lấy được danh mục", Toast.LENGTH_SHORT).show()
            }
        }
        mainRepository.getProducts { products: List<Product>? ->
            if (products != null) {
                binding.rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
                binding.rvItems.adapter = ProductAdapter(products)
            } else {
                Toast.makeText(requireContext(), "Không lấy được sản phẩm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun initListener() {
        onBackPressed {
            hideToolbar()
            popBackStack()
        }
        binding.ivCamera.singleClick {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }
        binding.ivFilter.singleClick {
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
    // ===== Dummy Data =====

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
