package com.pbl6.fitme.product

import Category
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProductDetailBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.profile.CategoryAdapter
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ProductDetailFragment : BaseFragment<FragmentProductDetailBinding, ProductDetailViewModel>() {

    private lateinit var imageAdapter: ProductImageAdapter

    private var currentProduct: Product? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideToolbar()
    }

    override fun initView() {
        setupRecyclerViews()
        imageAdapter = ProductImageAdapter()
        binding.rvImg.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }

        // ĐÃ XÓA: Khởi tạo colorAdapter và sizeAdapter
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        // Các nút này giờ sẽ mở BottomSheet
        binding.btnAddToCart.setOnClickListener(this)
        binding.btnBuyNow.setOnClickListener(this)

        binding.btnFavorite.setOnClickListener(this)

        viewModel.product.observe { product ->
            Log.d("ProductDetailFragment", "API Product Detail Response: $product")
            product?.let {
                currentProduct = it
                populateUI(it)
            }
        }

        viewModel.onAddToCartSuccess.observe { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                navigate(R.id.cartFragment)
            }
        }

        viewModel.onBuyNowSuccess.observe { success ->
            if (success) navigate(R.id.checkoutFragment)
        }

        viewModel.onAddToWishlistSuccess.observe { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào wishlist", Toast.LENGTH_SHORT).show()
                navigate(R.id.wishlistFragment)
            }
        }

        viewModel.errorMessage.observe { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // ĐÃ XÓA: Logic Quantity +/-
    }

    override fun initData() {
        val productId = arguments?.getString("productId") ?: return
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem chi tiết", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.fetchProductById(token, productId)
    }

    override fun onSingleClickFrag(v: View) {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        // ĐÃ XÓA: check selectedVariant

        when (v.id) {
            // Cả hai nút "Add to Cart" và "Buy Now" đều mở bottom sheet
            R.id.btnAddToCart-> {
                showAddtocardNowSheet()
            }
            R.id.btnBuyNow-> {
                showVariationsSheet()
            }
            R.id.btnFavorite -> {
                currentProduct?.let { viewModel.addToWishlist(token, it.productId) }
            }
        }
    }
    private fun showAddtocardNowSheet() {
        if (currentProduct == null) {
            Toast.makeText(requireContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            return
        }
        currentProduct?.let {
            val bottomSheet = AddToCartBottomSheetFragment.newInstance(it)
            bottomSheet.show(parentFragmentManager, "BuyNowBottomSheetFragment")
        }
    }
    private fun showVariationsSheet() {
        if (currentProduct == null) {
            Toast.makeText(requireContext(), "Đang tải dữ liệu sản phẩm...", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo và hiển thị BottomSheet, truyền dữ liệu product vào
        currentProduct?.let {
            val bottomSheet = VariationsBottomSheetFragment.newInstance(it)
            // TODO: Bạn có thể truyền thêm cờ "isBuyNow" nếu logic 2 nút khác nhau
            bottomSheet.show(parentFragmentManager, "VariationsBottomSheetFragment")
        }
    }


    private fun populateUI(p: Product) {
        imageAdapter.setList(p.images.map { it.imageUrl })
        binding.tvProductName.text = p.productName
        binding.tvDescription.text = p.description ?: ""
        p.variants.firstOrNull()?.let {
            binding.tvPrice.text = String.format("$%.2f", it.price)
        }
    }

    private fun hideToolbar() {
        activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
    }
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private fun setupRecyclerViews() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {

            val productAdapter = ProductAdapter()
            binding.rvRelatedProducts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            binding.rvRelatedProducts.adapter = productAdapter
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
}