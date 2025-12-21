package com.pbl6.fitme.voucher


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.databinding.ItemVoucherListBinding
import com.pbl6.fitme.model.Coupon
import hoang.dqm.codebase.utils.singleClick
import java.text.SimpleDateFormat
import java.util.Locale

class VoucherListAdapter : RecyclerView.Adapter<VoucherListAdapter.VoucherViewHolder>() {

    private var vouchers: List<Coupon> = emptyList()
    private var onUseClickListener: ((Coupon) -> Unit)? = null

    fun setList(list: List<Coupon>) {
        this.vouchers = list
        notifyDataSetChanged()
    }

    fun setOnUseClickListener(listener: (Coupon) -> Unit) {
        this.onUseClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(vouchers[position])
    }

    override fun getItemCount(): Int = vouchers.size

    inner class VoucherViewHolder(private val binding: ItemVoucherListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(coupon: Coupon) {
            // 1. Xử lý hiển thị Giá trị giảm (FIXED vs PERCENTAGE)
            if (coupon.discountType == "PERCENTAGE") {
                // Ví dụ: 10.0 -> 10% OFF
                val percent = coupon.discountValue.toInt()
                binding.txtVoucherPercent.text = "$percent% OFF"
            } else {
                // FIXED_AMOUNT: Ví dụ: $10.00 OFF
                val amount = String.format("%.2f", coupon.discountValue)
                binding.txtVoucherPercent.text = "$$amount OFF"
            }

            // 2. Hiển thị Mã code
            binding.txtVoucherCode.text = "CODE: ${coupon.code}"

            // 3. Hiển thị Điều kiện tối thiểu
            binding.txtVoucherCondition.text = "Min order $${String.format("%.0f", coupon.minimumOrderAmount)}"

            // 4. Hiển thị Ngày hết hạn
            // Giả sử expiryDate trả về String kiểu "yyyy-MM-dd" hoặc Long timestamp
            // Ở đây xử lý đơn giản hiển thị String
            binding.txtVoucherExpire.text = "Exp: ${formatDate(coupon.endDate)}"

            // 5. Xử lý nút Use
            binding.btnUse.singleClick {
                onUseClickListener?.invoke(coupon)
            }
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "N/A"
            // Tùy chỉnh format ngày tháng nếu cần
            return try {
                // Ví dụ input: 2025-12-20T00:00:00 -> output: 20/12/2025
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString.take(10)) // lấy 10 ký tự đầu yyyy-MM-dd
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString // Trả về nguyên gốc nếu parse lỗi
            }
        }
    }
}