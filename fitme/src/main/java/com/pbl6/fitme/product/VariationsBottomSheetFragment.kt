package com.pbl6.fitme.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl6.fitme.R
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductVariant
import hoang.dqm.codebase.base.activity.navigate
import java.io.Serializable

class VariationsBottomSheetFragment : BottomSheetDialogFragment() {

    // ViewModel được chia sẻ từ ProductDetailFragment
    private val viewModel: ProductDetailViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(ProductDetailViewModel::class.java)
    }

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
        // Inflate layout bạn đã tạo
        return inflater.inflate(R.layout.dialog_buy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy dữ liệu product từ arguments
        currentProduct = arguments?.getSerializable(ARG_PRODUCT) as? Product
        if (currentProduct == null) {
            dismiss()
            return
        }
        
        android.util.Log.d("VariationsBottomSheet", "Product loaded: id=${currentProduct?.productId}")
        android.util.Log.d("VariationsBottomSheet", "Available variants: ${currentProduct?.variants?.map { "${it.color}/${it.size}" }}")

        // Ánh xạ Views
        bindViews(view)

        // Khởi tạo Adapters và RecyclerViews
        initAdapters()
        initRecyclerViews()

        // Cài đặt Listeners
        initListeners()

        // Hiển thị dữ liệu
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
        colorAdapter = ColorAdapter(emptyList()) { color ->
            onColorSelected(color)
        }
        sizeAdapter = SizeAdapter(emptyList()) { size ->
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
        btnCloseSheet.setOnClickListener {
            dismiss() // Đóng dialog
        }

        btnBuyNowSheet.setOnClickListener {
            if (selectedVariant == null) {
                Toast.makeText(context, "Vui lòng chọn màu và size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Double check variant exists in product
            val existingVariant = currentProduct?.variants?.find { it.variantId == selectedVariant?.variantId }
            if (existingVariant == null) {
                android.util.Log.e("VariationsBottomSheet", "Selected variant ${selectedVariant?.variantId} not found in product variants")
                Toast.makeText(context, "Lỗi chọn sản phẩm, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate variant data
            android.util.Log.d("VariationsBottomSheet", "Validating variant: id=${existingVariant.variantId} color=${existingVariant.color} size=${existingVariant.size} stock=${existingVariant.stockQuantity}")

            // Lấy token: ưu tiên ViewModel (nếu được gán), nếu không có thì dùng SessionManager
            val token = viewModel.getAccessToken?.let { it1 -> it1(requireContext()) }
                ?: com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            android.util.Log.d("VariationsBottomSheet", "Token: ${token.take(10)}...")

            // Điều hướng trực tiếp tới Checkout với payload buy-now (product id, variant id, quantity)
            selectedVariant?.let { variant ->
                val quantity = tvQty.text.toString().toIntOrNull() ?: 1
                val productIdStr = currentProduct?.productId?.toString() ?: ""
                val variantIdStr = variant.variantId.toString()
                android.util.Log.d("VariationsBottomSheet", "BuyNow navigate: productId=$productIdStr variantId=$variantIdStr qty=$quantity")
                val bundle = android.os.Bundle().apply {
                    putString("buy_now_product_id", productIdStr)
                    putString("buy_now_variant_id", variantIdStr)
                    putInt("buy_now_quantity", quantity)
                    // pass selected size and color so Checkout can render quickly
                    putString("buy_now_size", selectedSize)
                    putString("buy_now_color", selectedColor)
                    // additionally pass visible product info (price, name, image) to avoid extra fetch
                    try {
                        putDouble("buy_now_price", variant.price)
                        putInt("buy_now_stock", variant.stockQuantity)  // Thêm stock để kiểm tra giới hạn
                    } catch (_: Exception) { putDouble("buy_now_price", 0.0) }
                    putString("buy_now_product_name", currentProduct?.productName ?: "")
                    putString("buy_now_image_url", currentProduct?.mainImageUrl ?: "")
                }
                // Điều hướng trực tiếp tới Checkout sử dụng NavController (đảm bảo bundle được truyền)
                navigate(R.id.checkoutFragment, bundle)
                
            }
            dismiss() // Đóng dialog
        }

        btnPlus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            val max = selectedVariant?.stockQuantity ?: 99
            if (current < max) {
                tvQty.text = (current + 1).toString()
            } else {
                Toast.makeText(context, "Đã đạt giới hạn tồn kho", Toast.LENGTH_SHORT).show()
            }
        }

        btnMinus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                tvQty.text = (current - 1).toString()
            }
        }
    }

    private fun populateUI() {
        currentProduct?.let { p ->
            // Tải ảnh đầu tiên của sản phẩm
            Glide.with(this)
                .load(p.images.firstOrNull()?.imageUrl)
                .into(ivProductSheet)

            if (p.variants.isNotEmpty()) {
                val allColors = p.variants.map { it.color }.distinct()
                val allSizes = p.variants.map { it.size }.distinct()

                colorAdapter.updateData(allColors)
                sizeAdapter.updateData(allSizes)

                // Tự động chọn size đầu tiên
                val defaultSize = allSizes.firstOrNull()
                if (defaultSize != null) {
                    sizeAdapter.setSelected(defaultSize)
                    onSizeSelected(defaultSize) // Thao tác này sẽ tự động chọn màu đầu tiên
                }
            }
        }
    }

    // --- SAO CHÉP LOGIC TỪ ProductDetailFragment SANG ---

    private fun onColorSelected(color: String) {
        android.util.Log.d("VariationsBottomSheet", "Color selected: $color")
        selectedColor = color
        val variants = currentProduct?.variants ?: return
        val availableSizes = variants.filter { it.color == color }.map { it.size }.distinct()
        android.util.Log.d("VariationsBottomSheet", "Available sizes for color $color: $availableSizes")
        sizeAdapter.updateAvailable(availableSizes)
        if (selectedSize !in availableSizes) {
            selectedSize = availableSizes.firstOrNull()
            android.util.Log.d("VariationsBottomSheet", "Auto-selecting first available size: $selectedSize")
            sizeAdapter.setSelected(selectedSize)
        }
        updatePriceAndVariant()
    }

    private fun onSizeSelected(size: String) {
        android.util.Log.d("VariationsBottomSheet", "Size selected: $size")
        selectedSize = size
        val variants = currentProduct?.variants ?: return
        val availableColors = variants.filter { it.size == size }.map { it.color }.distinct()
        android.util.Log.d("VariationsBottomSheet", "Available colors for size $size: $availableColors")
        colorAdapter.updateAvailable(availableColors)
        if (selectedColor !in availableColors) {
            selectedColor = availableColors.firstOrNull()
            android.util.Log.d("VariationsBottomSheet", "Auto-selecting first available color: $selectedColor")
            colorAdapter.setSelected(selectedColor)
        }
        updatePriceAndVariant()
    }

    private fun updatePriceAndVariant() {
        android.util.Log.d("VariationsBottomSheet", "Updating variant: color=$selectedColor size=$selectedSize")
        val variant = currentProduct?.variants?.find { it.color == selectedColor && it.size == selectedSize }
        if (variant != null) {
            selectedVariant = variant
            android.util.Log.d("VariationsBottomSheet", "Selected variant: id=${variant.variantId} price=${variant.price} stock=${variant.stockQuantity}")
            tvPriceSheet.text = variant.price.toString() // Định dạng tiền tệ
            tvStockSheet.text = "Kho: ${variant.stockQuantity}"
        } else {
            selectedVariant = null
            android.util.Log.d("VariationsBottomSheet", "No variant found for color=$selectedColor size=$selectedSize")
            tvPriceSheet.text = "Không có sẵn"
            tvStockSheet.text = "Kho: 0"
        }
    }

    // --- SAO CHÉP CÁC LỚP ADAPTER VÀO ĐÂY ---

    // (Sao chép y hệt lớp ColorAdapter từ ProductDetailFragment của bạn vào đây)
    class ColorAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.VH>() {
        // ... Dán toàn bộ code của ColorAdapter vào đây ...
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
            val txt: TextView = view.findViewById(R.id.tvVariation) // Đảm bảo R.id.tvVariation có trong item_variant.xml
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

    // (Sao chép y hệt lớp SizeAdapter từ ProductDetailFragment của bạn vào đây)
    class SizeAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<SizeAdapter.VH>() {
        // ... Dán toàn bộ code của SizeAdapter vào đây ...
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
            // LƯU Ý: Tôi đã xóa setOnClickListener ở đây vì bạn đã có nó trong khối init
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