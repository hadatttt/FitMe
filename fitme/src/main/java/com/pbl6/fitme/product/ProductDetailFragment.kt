package com.pbl6.fitme.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentProductDetailBinding
import com.pbl6.fitme.model.Product
import com.pbl6.fitme.model.ProductVariant
import hoang.dqm.codebase.base.activity.navigate

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val mainRepository = com.pbl6.fitme.repository.MainRepository()
    private var product: Product? = null
    private var selectedVariant: ProductVariant? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideToolbar()
        setupListeners()
        loadProduct()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
    private fun setupListeners() {
        binding.btnAddToCart.setOnClickListener {
            if (selectedVariant == null) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng chọn biến thể", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = com.pbl6.fitme.model.AddCartRequest(selectedVariant!!.variantId, 1)
            mainRepository.addToCart(token, req) { success ->
                activity?.runOnUiThread {
                    if (success) {
                        android.widget.Toast.makeText(requireContext(), "Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show()
                        navigate(R.id.cartFragment)
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Thêm vào giỏ hàng thất bại", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnBuyNow.setOnClickListener {
            if (selectedVariant == null) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng chọn biến thể", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập để đặt hàng", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = com.pbl6.fitme.model.AddCartRequest(selectedVariant!!.variantId, 1)
            mainRepository.addToCart(token, req) { success ->
                activity?.runOnUiThread {
                    if (success) {
                        navigate(R.id.checkoutFragment)
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Không thể đặt hàng", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnFavorite.setOnClickListener {
            // add to wishlist
            val prod = product ?: return@setOnClickListener
            val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
            if (token.isNullOrBlank()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm wishlist", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = com.pbl6.fitme.model.AddWishlistRequest(prod.productId)
            mainRepository.addToWishlist(token, req) { success ->
                activity?.runOnUiThread {
                    if (success) {
                        android.widget.Toast.makeText(requireContext(), "Đã thêm vào wishlist", android.widget.Toast.LENGTH_SHORT).show()
                        navigate(R.id.wishlistFragment)
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Thêm vào wishlist thất bại", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadProduct() {
        val productId = arguments?.getString("productId") ?: return
        val token = com.pbl6.fitme.session.SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem chi tiết", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        mainRepository.getProductById(token, productId) { p ->
            activity?.runOnUiThread {
                if (p == null) {
                    android.widget.Toast.makeText(requireContext(), "Không lấy được thông tin sản phẩm", android.widget.Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                product = p
                populateUI(p)
            }
        }
    }

    private fun populateUI(p: Product) {
        // main image
        val imgUrl = p.mainImageUrl
        if (!imgUrl.isNullOrBlank()) {
            Glide.with(binding.imgProduct.context).load(imgUrl).into(binding.imgProduct)
        }

        binding.tvDescription.text = p.description ?: ""
        // set product name if exists
        try {
            val nameView = binding.root.findViewById<TextView>(R.id.tvProductName)
            nameView?.text = p.productName
        } catch (_: Exception) { }

        // default price: first variant
        if (p.variants.isNotEmpty()) {
            selectedVariant = p.variants[0]
            binding.tvPrice.text = String.format("$%.2f", selectedVariant?.price)
        }

        // variants list
        val adapter = VariantAdapter(p.variants) { variant ->
            selectedVariant = variant
            binding.tvPrice.text = String.format("$%.2f", variant.price)
        }
        binding.rvVariations.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvVariations.adapter = adapter
    }

    // Simple adapter for variant selection
    class VariantAdapter(private val items: List<ProductVariant>, private val onSelect: (ProductVariant) -> Unit) :
        RecyclerView.Adapter<VariantAdapter.VH>() {

        private var selectedPos = RecyclerView.NO_POSITION

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val txt: TextView = view.findViewById(R.id.tvVariation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_variant, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val variant = items[position]
            holder.txt.text = "${variant.color} - ${variant.size}"
            // visual highlight
            if (position == selectedPos) {
                holder.txt.setBackgroundResource(R.drawable.bg_selected_variant)
            } else {
                holder.txt.setBackgroundResource(R.drawable.bg_outer_circle)
            }

            holder.itemView.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val prev = selectedPos
                selectedPos = pos
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(items[pos])
            }
        }

        override fun getItemCount() = items.size
    }
}
