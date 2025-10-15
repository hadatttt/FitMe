package com.pbl6.fitme.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProductDetailBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductVariant
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ProductDetailFragment : BaseFragment<FragmentProductDetailBinding, ProductDetailViewModel>() {

    private lateinit var imageAdapter: ProductImageAdapter
    private lateinit var colorAdapter: ColorAdapter
    private lateinit var sizeAdapter: SizeAdapter

    private var currentProduct: Product? = null
    private var selectedColor: String? = null
    private var selectedSize: String? = null
    private var selectedVariant: ProductVariant? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideToolbar()
    }

    override fun initView() {
        imageAdapter = ProductImageAdapter()
        binding.rvImg.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }

        colorAdapter = ColorAdapter(emptyList()) { color ->
            selectedColor = color
            updateSelectedVariant()
        }
        binding.rvColor.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvColor.adapter = colorAdapter

        sizeAdapter = SizeAdapter(emptyList()) { size ->
            selectedSize = size
            updateSelectedVariant()
        }
        binding.rvSize.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSize.adapter = sizeAdapter
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

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

        when (v.id) {
            R.id.btnAddToCart -> {
                selectedVariant?.let { viewModel.addToCart(token, it.variantId) }
                    ?: Toast.makeText(requireContext(), "Vui lòng chọn màu và size", Toast.LENGTH_SHORT).show()
            }
            R.id.btnBuyNow -> {
                selectedVariant?.let { viewModel.buyNow(token, it.variantId) }
                    ?: Toast.makeText(requireContext(), "Vui lòng chọn màu và size", Toast.LENGTH_SHORT).show()
            }
            R.id.btnFavorite -> {
                currentProduct?.let { viewModel.addToWishlist(token, it.productId) }
            }
        }
    }

    private fun populateUI(p: Product) {
        val imageUrls = p.images.map { it.imageUrl }
        imageAdapter.setList(imageUrls)

        binding.tvProductName.text = p.productName
        binding.tvDescription.text = p.description ?: ""

        if (p.variants.isNotEmpty()) {
            val colors = p.variants.map { it.color }.distinct()
            val sizes = p.variants.map { it.size }.distinct()

            colorAdapter.updateData(colors)
            sizeAdapter.updateData(sizes)

            selectedColor = colors.firstOrNull()
            selectedSize = sizes.firstOrNull()
            updateSelectedVariant()
        }
    }

    private fun updateSelectedVariant() {
        val variants = currentProduct?.variants ?: return
        val variant = variants.find { it.color == selectedColor && it.size == selectedSize }
        if (variant != null) {
            selectedVariant = variant
            binding.tvPrice.text = String.format("$%.2f", variant.price)
        }
    }

    private fun hideToolbar() {
        activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
    }

    class ColorAdapter(private var items: List<String>, private val onSelect: (String) -> Unit) :
        RecyclerView.Adapter<ColorAdapter.VH>() {

        private var selectedPos = if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION

        fun updateData(newItems: List<String>) {
            items = newItems
            selectedPos = if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_variant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val color = items[position]
            holder.txt.text = color
            holder.txt.setBackgroundResource(
                if (position == selectedPos) R.drawable.bg_selected_variant else R.drawable.bg_outer_circle
            )

            holder.itemView.setOnClickListener {
                val prev = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(items[selectedPos])
            }
        }

        override fun getItemCount() = items.size
    }

    // Adapter cho Size
    class SizeAdapter(private var items: List<String>, private val onSelect: (String) -> Unit) :
        RecyclerView.Adapter<SizeAdapter.VH>() {

        private var selectedPos = if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION

        fun updateData(newItems: List<String>) {
            items = newItems
            selectedPos = if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_variant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val size = items[position]
            holder.txt.text = size
            holder.txt.setBackgroundResource(
                if (position == selectedPos) R.drawable.bg_selected_variant else R.drawable.bg_outer_circle
            )

            holder.itemView.setOnClickListener {
                val prev = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(items[selectedPos])
            }
        }

        override fun getItemCount() = items.size
    }
}
