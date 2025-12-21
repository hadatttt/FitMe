package com.pbl6.fitme.checkin

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailycheckin.utils.DailyCheckIn
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentCheckInDialogBinding
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CheckInDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentCheckInDialogBinding
    private var adapter: CheckInAdapter? = null
    private var currentWeekStartIndex = 0

    // Khởi tạo Repository
    private val userRepository = UserRepository()
    private val viewModel: CheckInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCheckInDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenTransparentDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CheckInAdapter()
        binding.rvCheckIn.adapter = adapter
        binding.rvCheckIn.layoutManager = GridLayoutManager(requireContext(), 3)

        // --- 1. GỌI HÀM LẤY ĐIỂM NGAY KHI MỞ DIALOG ---
        fetchCurrentPoints()

        lifecycleScope.launch(Dispatchers.IO) {
            val fullList = DailyCheckIn.getListCheckIn(requireContext())
            val today = LocalDate.now()
            val indexToday = fullList.indexOfFirst { it.date == today }

            val currentCycle = if (indexToday >= 0) indexToday / 7 else 0
            currentWeekStartIndex = currentCycle * 7
            val endIndex = minOf(currentWeekStartIndex + 7, fullList.size)

            val currentWeekList = if (currentWeekStartIndex < fullList.size) {
                fullList.subList(currentWeekStartIndex, endIndex).toList()
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                adapter?.setOffset(currentWeekStartIndex)
                adapter?.updateListCheckIn(currentWeekList)
                if (currentWeekList.find { it.date == today }?.isChecked == true) {
                    disableClaimButton()
                }
            }
        }

        binding.btnClaim.singleClick {
            val todayDate = LocalDate.now()
            val todayItem = adapter?.dataList?.find { it.date == todayDate }
            if (todayItem?.isChecked == true) {
                disableClaimButton()
                return@singleClick
            }

            // Disable nút tạm thời để tránh spam click
            binding.btnClaim.isEnabled = false

            callCheckInApi(todayDate)
        }

        binding.btnClose.singleClick {
            dialog?.dismiss()
        }
    }

    // --- HÀM MỚI: Lấy điểm hiện tại hiển thị lên UI ---
    private fun fetchCurrentPoints() {
        val context = context ?: return
        val token = SessionManager.getInstance().getAccessToken(context)
        val userId = SessionManager.getInstance().getUserId(context).toString()

        if (!token.isNullOrEmpty() && userId.isNotEmpty()) {
            userRepository.getUserPoints(token, userId) { points ->
                // Cập nhật UI trên Main Thread
                activity?.runOnUiThread {
                    val currentPoints = points ?: 0
                    // Giả sử TextView hiển thị điểm tên là tvDiamond
                    binding.tvDiamondCollection.text = currentPoints.toString()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun callCheckInApi(date: LocalDate) {
        val context = requireContext()
        val token = SessionManager.getInstance().getAccessToken(context)
        val userId = SessionManager.getInstance().getUserId(context).toString()

        if (token.isNullOrEmpty() || userId.isEmpty()) {
            binding.btnClaim.isEnabled = true
            return
        }

        // BƯỚC 1: Lấy điểm hiện tại
        userRepository.getUserPoints(token, userId) { currentPoints ->
            val points = currentPoints ?: 0

            // BƯỚC 2: Tính toán điểm mới (Cộng 100)
            val newPoints = points + 200  // <--- Đây là số điểm đúng (ví dụ: 100)

            // BƯỚC 3: Gửi lên Server
            userRepository.updateUserPoints(token, userId, newPoints) { resultCode ->
                // resultCode trả về 0 (Thành công). Đừng dùng biến này để hiển thị điểm!

                    // --- THÀNH CÔNG ---
                    lifecycleScope.launch(Dispatchers.IO) {
                        DailyCheckIn.checkIn(context, date)

                        withContext(Dispatchers.Main) {

                            // --- SỬA Ở ĐÂY ---
                            binding.tvDiamondCollection.text = newPoints.toString()

                            updateUiAfterCheckIn()
                        }
                    }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUiAfterCheckIn() {
        disableClaimButton()
        adapter?.checkInNow()
        adapter?.notifyDataSetChanged()

        lifecycleScope.launch {
            delay(1000)
            if (isAdded && !isDetached && dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        }
    }

    private fun disableClaimButton() {
        binding.btnClaim.isEnabled = false
        binding.btnClaim.alpha = 0.5f // Làm mờ nút đi một chút
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    protected fun adjustInsetsForNavigation(viewBottom: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBottom) { view, insets ->
            try {
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                val systemInsets = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                val newBottomMargin = systemInsets.bottom
                if (params.bottomMargin != newBottomMargin) {
                    params.bottomMargin = newBottomMargin
                    view.layoutParams = params
                    view.requestLayout()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@setOnApplyWindowInsetsListener insets
        }
    }
}