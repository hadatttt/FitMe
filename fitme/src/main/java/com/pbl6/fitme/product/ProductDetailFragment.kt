package com.pbl6.fitme.product

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProductDetailBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.profile.ProductAdapter
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ProductDetailFragment : BaseFragment<FragmentProductDetailBinding, ProductDetailViewModel>() {

    private lateinit var imageAdapter: ProductImageAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var relatedProductAdapter: ProductAdapter

    private var currentProduct: Product? = null
    private var autoOpenAddToCart: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideToolbar()
    }

    override fun initView() {
        // Init Adapters
        imageAdapter = ProductImageAdapter()
        binding.rvImg.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }

        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        relatedProductAdapter = ProductAdapter()
        binding.rvRelatedProducts.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            adapter = relatedProductAdapter
        }

        // Click Related Product
        relatedProductAdapter.setOnClickItemRecyclerView { product, _ ->
            val bundle = Bundle().apply {
                putString("productId", product.productId.toString())
            }
            navigate(R.id.productDetailFragment, bundle)
        }
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.btnAddToCart.setOnClickListener(this)
        binding.btnBuyNow.setOnClickListener(this)
        binding.btnFavorite.setOnClickListener(this)

        binding.btnTry.singleClick {
            val imageUrl = currentProduct?.images?.firstOrNull()?.imageUrl
            if (imageUrl != null) {
                val bundle = Bundle().apply { putString("clothImageUrl", imageUrl) }
                navigate(R.id.aiViewFragment, bundle)
            }
        }


        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let {
                currentProduct = it
                populateUI(it)
                if (autoOpenAddToCart) {
                    autoOpenAddToCart = false
                    showAddToCartSheet()
                }
            }
        }

        // 2. Reviews
        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            if (!reviews.isNullOrEmpty()) {
                binding.rvReviews.visibility = View.VISIBLE
                binding.tvNoReviews.visibility = View.GONE
                reviewAdapter.setList(reviews)
            } else {
                binding.rvReviews.visibility = View.GONE
                binding.tvNoReviews.visibility = View.VISIBLE
            }
        }

        // 3. Related Products
        viewModel.relatedProducts.observe(viewLifecycleOwner) { products ->
            if (products != null) {
                relatedProductAdapter.setList(products)
            }
        }

        // 4. Wishlist Status
        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart else R.drawable.ic_heart_blue)
        }

        // 5. Toast Message
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.toastMessage.value = ""
            }
        }

        // 6. Cart / Buy Actions
        viewModel.onAddToCartSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                navigate(R.id.cartFragment)
                viewModel.onAddToCartSuccess.postValue(false)
            }
        }

        viewModel.onBuyNowSuccess.observe(viewLifecycleOwner) { success ->
            if (success) navigate(R.id.checkoutFragment)
            viewModel.onBuyNowSuccess.postValue(false)
        }
    }

    override fun initData() {
        val productId = arguments?.getString("productId") ?: return
        autoOpenAddToCart = arguments?.getBoolean("autoAddToCart") ?: false

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val userEmail = SessionManager.getInstance().getUserEmail(requireContext())

        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem chi tiết", Toast.LENGTH_SHORT).show()
            return
        }

        // Gọi ViewModel load data
        viewModel.loadData(token, productId, userEmail)
    }

    override fun onSingleClickFrag(v: View) {
        val token = SessionManager.getInstance().getAccessToken(requireContext()) ?: return

        when (v.id) {
            R.id.btnAddToCart -> showAddToCartSheet()
            R.id.btnBuyNow -> showVariationsSheet()
            R.id.btnFavorite -> {
                currentProduct?.let { p ->
                    val userEmail = SessionManager.getInstance().getUserEmail(requireContext())
                    viewModel.toggleWishlist(token, userEmail, p.productId.toString())
                }
            }
        }
    }

    private fun showAddToCartSheet() {
        currentProduct?.let { product ->
            // Reset cờ auto (dù không dùng nữa nhưng cứ reset để tránh lỗi logic sau này)
            autoOpenAddToCart = false

            // SỬA Ở ĐÂY: Chỉ truyền 1 tham số là product
            val bottomSheet = AddToCartBottomSheetFragment.newInstance(product)

            bottomSheet.show(parentFragmentManager, "AddToCartBottomSheet")
        } ?: Toast.makeText(requireContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
    }
    private fun showVariationsSheet() {
        currentProduct?.let {
            val bottomSheet = VariationsBottomSheetFragment.newInstance(it)
            bottomSheet.show(parentFragmentManager, "VariationsBottomSheet")
        } ?: Toast.makeText(requireContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
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
}