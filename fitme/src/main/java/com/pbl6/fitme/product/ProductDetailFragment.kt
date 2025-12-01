package com.pbl6.fitme.product

import com.pbl6.fitme.model.Category
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProductDetailBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.Review
import com.pbl6.fitme.profile.CategoryAdapter
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ProductDetailFragment : BaseFragment<FragmentProductDetailBinding, ProductDetailViewModel>() {

    private lateinit var imageAdapter: ProductImageAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private var currentProduct: Product? = null
    private var autoOpenAddToCart: Boolean = false

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
                if (autoOpenAddToCart) {
                    autoOpenAddToCart = false
                    showAddtocardNowSheet()
                }
            }
        }

        viewModel.onAddToCartSuccess.observe { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                navigate(R.id.cartFragment)
                viewModel.onAddToCartSuccess.postValue(false)
            }
        }

        viewModel.onBuyNowSuccess.observe { success ->
            if (success) navigate(R.id.checkoutFragment)
            viewModel.onBuyNowSuccess.postValue(false)
        }




        // ĐÃ XÓA: Logic Quantity +/-
    }

    override fun initData() {
        val productId = arguments?.getString("productId") ?: return
        autoOpenAddToCart = arguments?.getBoolean("autoAddToCart") ?: false
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem chi tiết", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.fetchProductById(token, productId)
        fetchProductReviews(token, productId)
    }

    override fun onSingleClickFrag(v: View) {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }


        when (v.id) {
            R.id.btnAddToCart-> {
                showAddtocardNowSheet()
            }
            R.id.btnBuyNow-> {
                showVariationsSheet()
            }
            R.id.btnFavorite -> {
                currentProduct?.let { product ->
                    val userEmail =
                        SessionManager.getInstance().getUserEmail(requireContext())

                    if (userEmail.isNullOrBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "Không tìm thấy thông tin người dùng",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val productId = product.productId // Lấy Product ID

                    android.util.Log.d(
                        "ProductDetailFragment",
                        "Add to wishlist initiated: userEmail=$userEmail, productId=$productId"
                    )

                    viewModel.addToWishlist(token, userEmail, productId)
                }
            }
        }
    }
    private fun showAddtocardNowSheet() {
        if (currentProduct == null) {
            Toast.makeText(requireContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            return
        }
        currentProduct?.let {
            val auto = autoOpenAddToCart
            autoOpenAddToCart = false
            val bottomSheet = AddToCartBottomSheetFragment.newInstance(it, auto)
            bottomSheet.show(parentFragmentManager, "BuyNowBottomSheetFragment")
        }
    }
    private fun showVariationsSheet() {
        if (currentProduct == null) {
            Toast.makeText(requireContext(), "Đang tải dữ liệu sản phẩm...", Toast.LENGTH_SHORT).show()
            return
        }

        currentProduct?.let {
            val bottomSheet = VariationsBottomSheetFragment.newInstance(it)
            bottomSheet.show(parentFragmentManager, "VariationsBottomSheetFragment")
        }
    }

  fun fetchProductReviews(token: String, productId: String) {
        reviewRepo.getReviewsByProduct(token, productId) { reviews: List<Review>? ->
            activity?.runOnUiThread {
                if (reviews != null && reviews.isNotEmpty()) {
                    Log.d("HomeFragment", "API Products Response: $reviews")
                    binding.rvReviews.visibility = View.VISIBLE
                    binding.tvNoReviews.visibility = View.GONE
                    reviewAdapter.setList(reviews)
                } else {
                    Log.d("HomeFragment", "API Products Response: $reviews")
                    binding.rvReviews.visibility = View.GONE
                    binding.tvNoReviews.visibility = View.VISIBLE
                }
            }
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
    private val reviewRepo = com.pbl6.fitme.repository.ReviewRepository()
    private fun setupRecyclerViews() {
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())

        if (!token.isNullOrBlank()) {

            // --- Setup Related Products (Giữ nguyên) ---
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

            // --- 🔽 MỚI: Setup Review Adapter ---
            // Gán cho biến class, không tạo biến local
            reviewAdapter = ReviewAdapter()
            binding.rvReviews.apply {
                adapter = reviewAdapter
                layoutManager = LinearLayoutManager(requireContext())
                isNestedScrollingEnabled = false
            }

        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem dữ liệu", Toast.LENGTH_LONG).show()
        }
    }
}