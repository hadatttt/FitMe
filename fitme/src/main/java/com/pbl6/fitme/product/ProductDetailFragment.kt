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
            onColorSelected(color)
        }
        binding.rvColor.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvColor.adapter = colorAdapter

        sizeAdapter = SizeAdapter(emptyList()) { size ->
            onSizeSelected(size)
        }
        binding.rvSize.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSize.adapter = sizeAdapter
    }

    override fun initListener() {
        binding.ivBack.singleClick { popBackStack() }

        binding.btnAddToCart.setOnClickListener(this)
        binding.btnBuyNow.setOnClickListener(this)
        binding.btnFavorite.setOnClickListener(this)

        viewModel.product.observe(viewLifecycleOwner) { product ->
            Log.d("ProductDetailFragment", "API Product Detail Response: $product")
            product?.let {
                currentProduct = it
                populateUI(it)
            }
        }

        viewModel.onAddToCartSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                navigate(R.id.cartFragment)
            }
        }

        viewModel.onBuyNowSuccess.observe(viewLifecycleOwner) { success ->
            if (success) navigate(R.id.checkoutFragment)
        }

        viewModel.onAddToWishlistSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã thêm vào wishlist", Toast.LENGTH_SHORT).show()
                navigate(R.id.wishlistFragment)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
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

        if (selectedVariant == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn màu và size hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        when (v.id) {
            R.id.btnAddToCart -> {
                selectedVariant?.let { viewModel.addToCart(token, it.variantId) }
            }
            R.id.btnBuyNow -> {
                selectedVariant?.let { viewModel.buyNow(token, it.variantId) }
            }
            R.id.btnFavorite -> {
                currentProduct?.let { viewModel.addToWishlist(token, it.productId) }
            }
        }
    }

    private fun populateUI(p: Product) {
        imageAdapter.setList(p.images.map { it.imageUrl })
        binding.tvProductName.text = p.productName
        binding.tvDescription.text = p.description ?: ""

        if (p.variants.isNotEmpty()) {
            val allColors = p.variants.map { it.color }.distinct()
            val allSizes = p.variants.map { it.size }.distinct()

            colorAdapter.updateData(allColors)
            sizeAdapter.updateData(allSizes)

            // Chọn giá trị mặc định và kích hoạt logic cập nhật
            // Ưu tiên chọn size trước, sau đó cập nhật màu khả dụng
            val defaultSize = allSizes.firstOrNull()
            if (defaultSize != null) {
                sizeAdapter.setSelected(defaultSize)
                onSizeSelected(defaultSize)
            }
        }
    }

    private fun onColorSelected(color: String) {
        selectedColor = color
        val variants = currentProduct?.variants ?: return

        // Tìm các size khả dụng cho màu đã chọn
        val availableSizes = variants.filter { it.color == color }.map { it.size }.distinct()
        sizeAdapter.updateAvailable(availableSizes)

        // Kiểm tra xem size đang chọn có còn khả dụng không. Nếu không, tự động chọn size đầu tiên.
        if (selectedSize !in availableSizes) {
            selectedSize = availableSizes.firstOrNull()
            sizeAdapter.setSelected(selectedSize)
        }
        updatePriceAndVariant()
    }

    private fun onSizeSelected(size: String) {
        selectedSize = size
        val variants = currentProduct?.variants ?: return

        // Tìm các màu khả dụng cho size đã chọn
        val availableColors = variants.filter { it.size == size }.map { it.color }.distinct()
        colorAdapter.updateAvailable(availableColors)

        // Kiểm tra xem màu đang chọn có còn khả dụng không. Nếu không, tự động chọn màu đầu tiên.
        if (selectedColor !in availableColors) {
            selectedColor = availableColors.firstOrNull()
            colorAdapter.setSelected(selectedColor)
        }
        updatePriceAndVariant()
    }

    private fun updatePriceAndVariant() {
        val variant = currentProduct?.variants?.find { it.color == selectedColor && it.size == selectedSize }
        if (variant != null) {
            selectedVariant = variant
            binding.tvPrice.text = String.format("$%.2f", variant.price)
        } else {
            selectedVariant = null
            binding.tvPrice.text = "Không có sẵn"
        }
    }

    private fun hideToolbar() {
        activity?.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
    }

    // Adapter cho Color
    class ColorAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.VH>() {

        private var selectedPos = RecyclerView.NO_POSITION
        private var availableItems: List<String> = items

        fun updateData(newItems: List<String>) {
            items = newItems
            availableItems = newItems
            selectedPos = RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        fun updateAvailable(available: List<String>) {
            availableItems = available
            notifyDataSetChanged()
        }

        fun setSelected(item: String?) {
            val newPos = items.indexOf(item)
            if (newPos != selectedPos) {
                val oldPos = selectedPos
                selectedPos = newPos
                if (oldPos >= 0) notifyItemChanged(oldPos)
                if (selectedPos >= 0) notifyItemChanged(selectedPos)
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        if (availableItems.contains(item)) {
                            if (position != selectedPos) {
                                val prev = selectedPos
                                selectedPos = position
                                if(prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
                                notifyItemChanged(selectedPos)
                                onSelect(item)
                            }
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_variant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val isAvailable = availableItems.contains(item)

            holder.txt.text = item
            holder.txt.isEnabled = isAvailable
            holder.txt.alpha = if (isAvailable) 1f else 0.4f

            holder.txt.setBackgroundResource(
                if (position == selectedPos && isAvailable)
                    R.drawable.bg_selected_variant
                else
                    R.drawable.bg_outer_circle
            )
        }

        override fun getItemCount() = items.size
    }

    // Adapter cho Size
    class SizeAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<SizeAdapter.VH>() {

        private var selectedPos = RecyclerView.NO_POSITION
        private var availableItems: List<String> = items

        fun updateData(newItems: List<String>) {
            items = newItems
            availableItems = newItems
            selectedPos = RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        fun updateAvailable(available: List<String>) {
            availableItems = available
            notifyDataSetChanged()
        }

        fun setSelected(item: String?) {
            val newPos = items.indexOf(item)
            if (newPos != selectedPos) {
                val oldPos = selectedPos
                selectedPos = newPos
                if (oldPos >= 0) notifyItemChanged(oldPos)
                if (selectedPos >= 0) notifyItemChanged(selectedPos)
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        if (availableItems.contains(item)) {
                            if (position != selectedPos) {
                                val prev = selectedPos
                                selectedPos = position
                                if(prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
                                notifyItemChanged(selectedPos)
                                onSelect(item)
                            }
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_variant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val isAvailable = availableItems.contains(item)

            holder.txt.text = item
            holder.txt.isEnabled = isAvailable
            holder.txt.alpha = if (isAvailable) 1f else 0.4f

            holder.txt.setBackgroundResource(
                if (position == selectedPos && isAvailable)
                    R.drawable.bg_selected_variant
                else
                    R.drawable.bg_outer_circle
            )
        }
        override fun getItemCount() = items.size
    }
}