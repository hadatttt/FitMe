package com.pbl6.fitme.product

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl6.fitme.R
import com.pbl6.fitme.model.CartItem
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductVariant
import com.pbl6.fitme.session.SessionManager
import java.io.Serializable
import java.util.UUID

class AddToCartBottomSheetFragment : BottomSheetDialogFragment() {

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
    private lateinit var btnAddToCartSheet: Button
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

        // 1. Safe data retrieval
        arguments?.let {
            currentProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_PRODUCT, Product::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_PRODUCT) as? Product
            }
        }

        if (currentProduct == null) {
            Toast.makeText(context, "Could not load product", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        bindViews(view)

        // Update button text to English
        btnAddToCartSheet.text = "Add to Cart"

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
        btnAddToCartSheet = view.findViewById(R.id.btnBuyNowSheet)
        btnMinus = view.findViewById(R.id.btnMinusDetail)
        btnPlus = view.findViewById(R.id.btnPlusDetail)
        tvQty = view.findViewById(R.id.tvQtyDetail)
    }

    private fun initAdapters() {
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

        btnAddToCartSheet.setOnClickListener {
            handleAddToCart()
        }

        btnPlus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            val max = selectedVariant?.stockQuantity ?: 99
            if (current < max) {
                tvQty.text = (current + 1).toString()
            } else {
                Toast.makeText(context, "Only $max items left", Toast.LENGTH_SHORT).show()
            }
        }

        btnMinus.setOnClickListener {
            val current = tvQty.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                tvQty.text = (current - 1).toString()
            }
        }
    }

    private fun handleAddToCart() {
        if (selectedVariant == null) {
            Toast.makeText(context, "Please select color and size", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = tvQty.text.toString().toIntOrNull() ?: 1
        val session = SessionManager.getInstance()
        val context = requireContext()

        // 1. Get or Create Cart ID
        val cartId = session.getOrCreateCartId(context)

        // 2. Create CartItem object
        val cartItem = CartItem(
            cartItemId = UUID.randomUUID(),
            cartId = cartId,
            variantId = selectedVariant!!.variantId,
            quantity = quantity,
            addedAt = System.currentTimeMillis().toString()
        )

        // 3. Save to Local Session
        session.addToCartLocal(context, cartItem)

        // 4. Feedback and close
        Toast.makeText(context, "Added to cart successfully", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun populateUI() {
        currentProduct?.let { p ->
            Glide.with(requireContext())
                .load(p.images.firstOrNull()?.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(ivProductSheet)

            if (p.variants.isNotEmpty()) {
                val allColors = p.variants.map { it.color }.distinct()
                val allSizes = p.variants.map { it.size }.distinct()

                colorAdapter.updateData(allColors)
                sizeAdapter.updateData(allSizes)

                val defaultSize = allSizes.firstOrNull()
                if (defaultSize != null) {
                    sizeAdapter.setSelected(defaultSize)
                    onSizeSelected(defaultSize)
                }
            } else {
                tvPriceSheet.text = "Out of stock"
                btnAddToCartSheet.isEnabled = false
            }
        }
    }

    private fun onColorSelected(color: String) {
        selectedColor = color
        val variants = currentProduct?.variants ?: return
        val availableSizes = variants.filter { it.color == color }.map { it.size }.distinct()
        sizeAdapter.updateAvailable(availableSizes)

        if (selectedSize !in availableSizes) {
            selectedSize = availableSizes.firstOrNull()
            sizeAdapter.setSelected(selectedSize)
        }
        updatePriceAndVariant()
    }

    private fun onSizeSelected(size: String) {
        selectedSize = size
        val variants = currentProduct?.variants ?: return
        val availableColors = variants.filter { it.size == size }.map { it.color }.distinct()
        colorAdapter.updateAvailable(availableColors)

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
            tvStockSheet.text = "Stock: ${variant.stockQuantity}"
            btnAddToCartSheet.isEnabled = variant.stockQuantity > 0
        } else {
            selectedVariant = null
            tvPriceSheet.text = "Not available"
            tvStockSheet.text = "Stock: -"
            btnAddToCartSheet.isEnabled = false
        }
        tvQty.text = "1"
    }

    // ================== ADAPTER CLASSES ==================

    class ColorAdapter(
        private var items: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.VH>() {

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
            if (newPos != selectedPos) {
                selectedPos = newPos
                notifyDataSetChanged()
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
            } else {
                holder.txt.setBackgroundResource(R.drawable.bg_variant)
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
            } else {
                holder.txt.setBackgroundResource(R.drawable.bg_variant)
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val ARG_PRODUCT = "arg_product"

        fun newInstance(product: Product): AddToCartBottomSheetFragment {
            return AddToCartBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PRODUCT, product as Serializable)
                }
            }
        }
    }
}