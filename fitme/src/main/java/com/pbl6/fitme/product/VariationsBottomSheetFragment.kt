package com.pbl6.fitme.product

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductVariant
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.navigate
import java.io.Serializable

class VariationsBottomSheetFragment : BottomSheetDialogFragment() {

    // Bỏ ViewModel, dùng trực tiếp SessionManager để tránh lỗi requireParentFragment()

    private var currentProduct: Product? = null
    private var selectedColor: String? = null
    private var selectedSize: String? = null
    private var selectedVariant: ProductVariant? = null

    private lateinit var colorAdapter: ColorAdapter
    private lateinit var sizeAdapter: SizeAdapter

    // Views
    private lateinit var ivProductSheet: ImageView
    private lateinit var tvPriceSheet: TextView
    private lateinit var tvStockSheet: TextView
    private lateinit var rvSize: RecyclerView
    private lateinit var rvColor: RecyclerView
    private lateinit var btnCloseSheet: ImageButton
    private lateinit var btnBuyNowSheet: Button
    private lateinit var btnMinus: ImageButton
    private lateinit var btnPlus: ImageButton
    private lateinit var tvQty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_buy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Lấy dữ liệu an toàn
        arguments?.let {
            currentProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_PRODUCT, Product::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_PRODUCT) as? Product
            }
        }

        if (currentProduct == null) {
            Toast.makeText(context, "Không tải được sản phẩm", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        bindViews(view)
        initAdapters()
        initRecyclerViews()
        initListeners()
        populateUI()
    }

    private fun bindViews(view: View) {
        ivProductSheet = view.findViewById(R.id.ivProductSheet)
        tvPriceSheet = view.findViewById(R.id.tvPriceSheet)
        tvStockSheet = view.findViewById(R.id.tvStockSheet)
        rvSize = view.findViewById(R.id.rvSize)
        rvColor = view.findViewById(R.id.rvColor)
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet)
        btnBuyNowSheet = view.findViewById(R.id.btnBuyNowSheet)
        btnMinus = view.findViewById(R.id.btnMinusDetail)
        btnPlus = view.findViewById(R.id.btnPlusDetail)
        tvQty = view.findViewById(R.id.tvQtyDetail)
    }

    private fun initAdapters() {
        // Khởi tạo Adapter với list rỗng ban đầu
        colorAdapter = ColorAdapter(arrayListOf()) { color ->
            onColorSelected(color)
        }
        sizeAdapter = SizeAdapter(arrayListOf()) { size ->
            onSizeSelected(size)
        }
    }

    private fun initRecyclerViews() {
        rvColor.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvColor.adapter = colorAdapter

        rvSize.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvSize.adapter = sizeAdapter
    }

    private fun initListeners() {
        btnCloseSheet.setOnClickListener { dismiss() }

        btnBuyNowSheet.setOnClickListener {
            handleBuyNow()
        }

        btnPlus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            val max = selectedVariant?.stockQuantity ?: 99
            if (current < max) {
                tvQty.text = (current + 1).toString()
            } else {
                Toast.makeText(context, "Chỉ còn $max sản phẩm", Toast.LENGTH_SHORT).show()
            }
        }

        btnMinus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                tvQty.text = (current - 1).toString()
            }
        }
    }

    private fun handleBuyNow() {
        if (selectedVariant == null) {
            Toast.makeText(context, "Vui lòng chọn màu và size", Toast.LENGTH_SHORT).show()
            return
        }

        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show()
            return
        }

        selectedVariant?.let { variant ->
            val quantity = tvQty.text.toString().toIntOrNull() ?: 1

            val bundle = Bundle().apply {
                putString("buy_now_product_id", currentProduct?.productId.toString())
                putString("buy_now_variant_id", variant.variantId.toString())
                putInt("buy_now_quantity", quantity)
                // Các thông tin phụ trợ để hiển thị nhanh bên checkout
                putString("buy_now_size", selectedSize)
                putString("buy_now_color", selectedColor)
                putDouble("buy_now_price", variant.price)
                putString("buy_now_product_name", currentProduct?.productName ?: "")
                putString("buy_now_image_url", currentProduct?.images?.firstOrNull()?.imageUrl ?: "")
            }

            // Navigate
            try {
                navigate(R.id.checkoutFragment, bundle)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun populateUI() {
        currentProduct?.let { p ->
            Glide.with(requireContext()) // Dùng requireContext an toàn hơn 'this'
                .load(p.images.firstOrNull()?.imageUrl)
                .placeholder(R.drawable.ic_launcher_background) // Thêm placeholder nếu cần
                .into(ivProductSheet)

            if (p.variants.isNotEmpty()) {
                val allColors = p.variants.map { it.color }.distinct()
                val allSizes = p.variants.map { it.size }.distinct()

                colorAdapter.updateData(allColors)
                sizeAdapter.updateData(allSizes)

                // Auto select logic
                val defaultSize = allSizes.firstOrNull()
                if (defaultSize != null) {
                    sizeAdapter.setSelected(defaultSize)
                    onSizeSelected(defaultSize)
                }
            } else {
                tvPriceSheet.text = "Hết hàng"
                btnBuyNowSheet.isEnabled = false
            }
        }
    }

    private fun onColorSelected(color: String) {
        selectedColor = color
        val variants = currentProduct?.variants ?: return

        // Filter sizes available for this color
        val availableSizes = variants.filter { it.color == color }.map { it.size }.distinct()
        sizeAdapter.updateAvailable(availableSizes)

        // Reset size selection if current size not available in this color
        if (selectedSize !in availableSizes) {
            selectedSize = availableSizes.firstOrNull()
            sizeAdapter.setSelected(selectedSize)
        }
        updatePriceAndVariant()
    }

    private fun onSizeSelected(size: String) {
        selectedSize = size
        val variants = currentProduct?.variants ?: return

        // Filter colors available for this size
        val availableColors = variants.filter { it.size == size }.map { it.color }.distinct()
        colorAdapter.updateAvailable(availableColors)

        // Reset color selection if current color not available in this size
        if (selectedColor !in availableColors) {
            selectedColor = availableColors.firstOrNull()
            colorAdapter.setSelected(selectedColor)
        }
        updatePriceAndVariant()
    }

    private fun updatePriceAndVariant() {
        val variant = currentProduct?.variants?.find {
            it.color == selectedColor && it.size == selectedSize
        }

        if (variant != null) {
            selectedVariant = variant
            tvPriceSheet.text = String.format("$%.2f", variant.price)
            tvStockSheet.text = "Kho: ${variant.stockQuantity}"
            btnBuyNowSheet.isEnabled = variant.stockQuantity > 0
        } else {
            selectedVariant = null
            tvPriceSheet.text = "Không có sẵn"
            tvStockSheet.text = "Kho: -"
            btnBuyNowSheet.isEnabled = false
        }

        // Reset qty về 1 mỗi khi đổi variant
        tvQty.text = "1"
    }

    // ================== ADAPTER CLASSES (FULL CODE) ==================

    class ColorAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.VH>() {

        private var selectedPos = RecyclerView.NO_POSITION
        private var availableItems: List<String> = items
        private var selectedItem: String? = null

        fun updateData(newItems: List<String>) {
            items = newItems
            availableItems = newItems // Reset available
            selectedPos = RecyclerView.NO_POSITION
            selectedItem = null
            notifyDataSetChanged()
        }

        fun updateAvailable(available: List<String>) {
            availableItems = available
            notifyDataSetChanged()
        }

        fun setSelected(item: String?) {
            selectedItem = item
            val newPos = items.indexOf(item)
            if (newPos != selectedPos) {
                selectedPos = newPos
                notifyDataSetChanged() // Refresh toàn bộ để vẽ lại background
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        // Chỉ cho click nếu có hàng
                        if (availableItems.contains(item)) {
                            setSelected(item)
                            onSelect(item)
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
            val isSelected = (item == selectedItem)

            holder.txt.text = item
            holder.txt.isEnabled = isAvailable
            holder.txt.alpha = if (isAvailable) 1.0f else 0.3f

            // Xử lý background
            if (isSelected && isAvailable) {
                holder.txt.setBackgroundResource(R.drawable.bg_selected_variant) // File drawable viền màu/đậm
                holder.txt.setTextColor(Color.WHITE) // Ví dụ: chọn thì chữ trắng
            } else {
                holder.txt.setBackgroundResource(R.drawable.bg_outer_circle) // File drawable viền thường
                holder.txt.setTextColor(Color.BLACK)
            }
        }

        override fun getItemCount() = items.size
    }

    class SizeAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<SizeAdapter.VH>() {

        private var selectedPos = RecyclerView.NO_POSITION
        private var availableItems: List<String> = items
        private var selectedItem: String? = null

        fun updateData(newItems: List<String>) {
            items = newItems
            availableItems = newItems
            selectedPos = RecyclerView.NO_POSITION
            selectedItem = null
            notifyDataSetChanged()
        }

        fun updateAvailable(available: List<String>) {
            availableItems = available
            notifyDataSetChanged()
        }

        fun setSelected(item: String?) {
            selectedItem = item
            val newPos = items.indexOf(item)
            selectedPos = newPos
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        if (availableItems.contains(item)) {
                            setSelected(item)
                            onSelect(item)
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
            val isSelected = (item == selectedItem)

            holder.txt.text = item
            holder.txt.isEnabled = isAvailable
            holder.txt.alpha = if (isAvailable) 1.0f else 0.3f

            if (isSelected && isAvailable) {
                holder.txt.setBackgroundResource(R.drawable.bg_selected_variant)
                holder.txt.setTextColor(Color.WHITE)
            } else {
                holder.txt.setBackgroundResource(R.drawable.bg_outer_circle)
                holder.txt.setTextColor(Color.BLACK)
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val ARG_PRODUCT = "arg_product"

        fun newInstance(product: Product): VariationsBottomSheetFragment {
            return VariationsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PRODUCT, product as Serializable)
                }
            }
        }
    }
}