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
import com.pbl6.fitme.session.SessionManager
import java.io.Serializable

// FILE MỚI: Chỉ dành cho Add to Cart
class AddToCartBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: ProductDetailViewModel by lazy {
        ViewModelProvider(requireParentFragment()).get(ProductDetailViewModel::class.java)
    }

    private var currentProduct: Product? = null
    private var selectedColor: String? = null
    private var selectedSize: String? = null
    private var selectedVariant: ProductVariant? = null
    private var autoAdd: Boolean = false

    private lateinit var colorAdapter: VariationsBottomSheetFragment.ColorAdapter
    private lateinit var sizeAdapter: VariationsBottomSheetFragment.SizeAdapter

    // Views
    private lateinit var ivProductSheet: ImageView
    private lateinit var tvPriceSheet: TextView
    private lateinit var tvStockSheet: TextView
    private lateinit var rvSize: RecyclerView
    private lateinit var rvColor: RecyclerView
    private lateinit var btnCloseSheet: ImageButton
    private lateinit var btnAddToCartSheet: Button // Nút hành động
    private lateinit var btnMinus: ImageButton
    private lateinit var btnPlus: ImageButton
    private lateinit var tvQty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Dùng chung layout
        return inflater.inflate(R.layout.dialog_add_to_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentProduct = arguments?.getSerializable(ARG_PRODUCT) as? Product
        autoAdd = arguments?.getBoolean(ARG_AUTO_ADD) ?: false
        if (currentProduct == null) {
            dismiss()
            return
        }

        bindViews(view)

        // Cập nhật text của nút cho đúng
        btnAddToCartSheet.text = "Thêm vào Giỏ hàng"

        initAdapters()
        initRecyclerViews()
        initListeners()
        populateUI()

        // If opened with autoAdd flag, and a variant is already selected, perform add-to-cart automatically
        if (autoAdd) {
            // Ensure selection and price updated
            if (selectedVariant == null) {
                // Attempt to pick first available variant
                val pick = currentProduct?.variants?.firstOrNull()
                if (pick != null) {
                    selectedVariant = pick
                }
            }

            // If selectedVariant is available, perform the same logic as clicking the button
            selectedVariant?.let {
                val token = SessionManager.getInstance().getAccessToken(requireContext())
                if (!token.isNullOrBlank()) {
                    val quantity = tvQty.text.toString().toIntOrNull() ?: 1
                    viewModel.addToCart(requireContext(), token, it.variantId, quantity)
                }
            }
            dismiss()
            return
        }
    }

    private fun bindViews(view: View) {
        ivProductSheet = view.findViewById(R.id.ivProductSheet)
        tvPriceSheet = view.findViewById(R.id.tvPriceSheet)
        tvStockSheet = view.findViewById(R.id.tvStockSheet)
        rvSize = view.findViewById(R.id.rvSize)
        rvColor = view.findViewById(R.id.rvColor)
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet)
        btnAddToCartSheet = view.findViewById(R.id.btnAddToCartSheet)
        btnMinus = view.findViewById(R.id.btnMinusDetail)
        btnPlus = view.findViewById(R.id.btnPlusDetail)
        tvQty = view.findViewById(R.id.tvQtyDetail)
    }

    private fun initAdapters() {
        colorAdapter =
            VariationsBottomSheetFragment.ColorAdapter(emptyList()) { color -> onColorSelected(color) }
        sizeAdapter =
            VariationsBottomSheetFragment.SizeAdapter(emptyList()) { size -> onSizeSelected(size) }
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
            if (selectedVariant == null) {
                Toast.makeText(context, "Vui lòng chọn màu và size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // CHỈ GỌI AddToCart (với fallback lưu local nếu network thất bại)
            selectedVariant?.let {
                val quantity = tvQty.text.toString().toIntOrNull() ?: 1
                viewModel.addToCart(requireContext(), token, it.variantId, quantity)
            }
            dismiss()
        }

        // ... (Listeners cho btnPlus, btnMinus giữ nguyên như cũ) ...
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

    // ... (Toàn bộ các hàm populateUI, onColorSelected, onSizeSelected, updatePriceAndVariant giữ nguyên) ...
    private fun populateUI() {
        currentProduct?.let { p ->
            Glide.with(this)
                .load(p.images.firstOrNull()?.imageUrl)
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
        val variant = currentProduct?.variants?.find { it.color == selectedColor && it.size == selectedSize }
        if (variant != null) {
            selectedVariant = variant
            tvPriceSheet.text = String.format("%.0fđ", variant.price)
            tvStockSheet.text = "Kho: ${variant.stockQuantity}"
        } else {
            selectedVariant = null
            tvPriceSheet.text = "Không có sẵn"
            tvStockSheet.text = "Kho: 0"
        }
        tvQty.text = "1"
    }



    companion object {
        private const val ARG_PRODUCT = "arg_product"
        private const val ARG_AUTO_ADD = "arg_auto_add"

        fun newInstance(product: Product, autoAdd: Boolean = false): AddToCartBottomSheetFragment {
            return AddToCartBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PRODUCT, product as Serializable)
                    putBoolean(ARG_AUTO_ADD, autoAdd)
                }
            }
        }
    }
}