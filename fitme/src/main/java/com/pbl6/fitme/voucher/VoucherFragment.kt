package com.pbl6.fitme.voucher

import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentVoucherBinding
import com.pbl6.fitme.home.HomeMainViewModel
import com.pbl6.fitme.repository.CouponRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class VoucherFragment : BaseFragment<FragmentVoucherBinding, HomeMainViewModel>() {

    private val couponRepository = CouponRepository()
    private lateinit var voucherAdapter: VoucherListAdapter

    override fun initView() {
        // Ẩn Toolbar của Activity chính nếu cần
        requireActivity().findViewById<View>(R.id.toolbar)?.visibility = View.GONE

        // Setup RecyclerView
        voucherAdapter = VoucherListAdapter()
        binding.rvVoucherList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVoucherList.adapter = voucherAdapter
    }

    override fun initData() {
        loadAllCoupons()
    }

    private fun loadAllCoupons() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login to view vouchers", Toast.LENGTH_SHORT).show()
            return
        }

        // Gọi API lấy danh sách coupon
        couponRepository.getAllCoupons(token) { coupons ->
            activity?.runOnUiThread {
                if (!coupons.isNullOrEmpty()) {
                    // Lọc chỉ lấy coupon đang Active (nếu cần)
                    val activeCoupons = coupons.filter { it.isActive }

                    if (activeCoupons.isNotEmpty()) {
                        binding.rvVoucherList.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                        voucherAdapter.setList(activeCoupons)
                    } else {
                        showEmptyState()
                    }
                } else {
                    showEmptyState()
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.rvVoucherList.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
    }

    override fun initListener() {
        // Nút Back
        binding.ivBack.singleClick {
            popBackStack()
        }

        // Xử lý khi bấm nút "Use" trên từng Voucher
        voucherAdapter.setOnUseClickListener { coupon ->
            // Logic: Copy mã voucher (opsional) hoặc chỉ đơn giản là về Home để mua sắm
            Toast.makeText(requireContext(), "Start shopping with code: ${coupon.code}", Toast.LENGTH_SHORT).show()

            // Navigate đến HomeFragment
            navigate(R.id.homeFragment)

            // Nếu bạn dùng BottomNavigationView, bạn có thể cần update selected tab
            // val bottomNav = requireActivity().findViewById<View>(R.id.bottom_nav) ...
        }
    }
}