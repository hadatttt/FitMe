package com.pbl6.fitme.checkout

import android.annotation.SuppressLint
import com.pbl6.fitme.databinding.ItemVoucherSmallBinding
import com.pbl6.fitme.model.Coupon
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import java.text.SimpleDateFormat
import java.util.Locale

class CouponAdapter : BaseRecyclerViewAdapter<Coupon, ItemVoucherSmallBinding>() {

    @SuppressLint("SetTextI18n")
    override fun bindData(
        binding: ItemVoucherSmallBinding,
        item: Coupon,
        position: Int
    ) {
        // 1. Hiển thị mức giảm giá (Tiêu đề)
        if (item.discountType == "PERCENTAGE") {
            binding.txtVoucherPercent.text = "${item.discountValue.toInt()}% OFF"
        } else {
            // FIXED_AMOUNT
            binding.txtVoucherPercent.text = "$${String.format("%.2f", item.discountValue)} OFF"
        }

        // 2. Hiển thị điều kiện đơn hàng tối thiểu
        if (item.minimumOrderAmount > 0) {
            binding.txtVoucherCondition.text = "Min order $${String.format("%.0f", item.minimumOrderAmount)}"
        } else {
            binding.txtVoucherCondition.text = "No min spend"
        }

        // 3. Hiển thị ngày hết hạn (Giả sử định dạng từ server là ISO hoặc tương tự, ở đây hiển thị text gốc hoặc format lại nếu cần)
        // Nếu chuỗi date là "2025-12-20T...", bạn có thể cắt chuỗi hoặc dùng SimpleDateFormat
        if (!item.endDate.isNullOrEmpty()) {
            try {
                // Ví dụ đơn giản lấy 10 ký tự đầu YYYY-MM-DD
                val dateStr = item.endDate.take(10)
                binding.txtVoucherExpire.text = "Exp: $dateStr"
            } catch (e: Exception) {
                binding.txtVoucherExpire.text = "Exp: ${item.endDate}"
            }
        } else {
            binding.txtVoucherExpire.text = "No expiry"
        }

        // 4. Xử lý click (BaseRecyclerViewAdapter thường đã có setOnItemClickListener, nhưng nếu cần custom visual khi chọn)
        binding.root.alpha = if (item.isActive) 1.0f else 0.5f
    }
}