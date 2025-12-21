package com.pbl6.fitme.slotmachinegame

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.slotmachinegame.imageviewscrolling.SlotData
import com.example.slotmachinegame.imageviewscrolling.SlotMachineView
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentSlotMachineGameBinding
import com.pbl6.fitme.repository.UserRepository // 1. Thêm Repo
import com.pbl6.fitme.session.SessionManager // 2. Thêm Session
import com.pbl6.fitme.untils.AppSharePref
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SlotMachineGameFragment : BaseFragment<FragmentSlotMachineGameBinding, SlotMachineGameViewModel>() {

    private val sharedViewModel: SharedNavigationViewModel by activityViewModels()

    // Khai báo UserRepository
    private val userRepository = UserRepository()

    private val listBonusSymbol = listOf(
        SlotData(0, R.drawable.ic_500),
        SlotData(1, R.drawable.ic_1000),
        SlotData(2, R.drawable.ic_2000),
        SlotData(3, R.drawable.ic_5000),
        SlotData(4, R.drawable.ic_500),
        SlotData(5, R.drawable.ic_1000),
    )

    private var isSpinning = false

    override fun initView() {
        context?.let {
            // Spin vẫn lấy từ Local (hoặc bạn muốn lấy từ API thì sửa tương tự)
            viewModel.setSpinsLeft(AppSharePref(it).spinCount)

            // Diamond (Coin) lấy từ API thay vì AppSharePref
            fetchUserPoints()
        }

        binding.slotMachine.changeState(isSpinning)
        binding.slotMachine.setBonusSymbols(listBonusSymbol)

        viewModel.spinsLeft.observe(viewLifecycleOwner) { spinsLeft ->
            updateSpinUI(spinsLeft)
        }

        viewModel.diamondCount.observe(viewLifecycleOwner) { diamondCount ->
            binding.tvDiamond.text = diamondCount.toString()
        }

        binding.slotMachine.setSpinningMachineCallBack(object :
            SlotMachineView.SpinningMachineCallBack {

            override fun startSpinning() {
                isSpinning = true
                binding.slotMachine.changeState(isSpinning)
                updateSpinUI(viewModel.spinsLeft.value ?: 0)
            }

            override fun endSpinning(value1: Int, value2: Int, value3: Int) {
                viewModel.decrementSpinsLeft()

                if (value1 == value2 && value2 == value3) {
                    lifecycleScope.launch {
                        delay(500)
                        val reward = listBonusSymbol.getOrNull(value1)
                        reward?.let { showReward(it) }
                        isSpinning = false
                        binding.slotMachine.changeState(isSpinning)
                        updateSpinUI(viewModel.spinsLeft.value ?: 0)
                    }
                } else {
                    isSpinning = false
                    binding.slotMachine.changeState(isSpinning)
                    updateSpinUI(viewModel.spinsLeft.value ?: 0)
                }
            }
        })
    }

    // --- HÀM LẤY ĐIỂM TỪ API ---
    private fun fetchUserPoints() {
        val token = SessionManager.getInstance().getAccessToken(requireContext())
        val userId = SessionManager.getInstance().getUserId(requireContext())?.toString()

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            userRepository.getUserPoints(token, userId) { points ->
                activity?.runOnUiThread {
                    val currentPoints = points ?: 0
                    viewModel.setDiamondCount(currentPoints)

                    context?.let { ctx -> AppSharePref(ctx).diamondCount = currentPoints }
                }
            }
        } else {
            context?.let { viewModel.setDiamondCount(AppSharePref(it).diamondCount) }
        }
    }

    // --- HÀM LƯU ĐIỂM LÊN API (Cần Backend hỗ trợ API update points) ---
    private fun savePointsToApi(addedPoints: Int) {
        // Lưu ý: Hiện tại UserRepository của bạn chưa có hàm cộng điểm (Add Points).
        // Bạn chỉ có hàm update thông tin user hoặc lấy điểm.
        // Tạm thời mình sẽ chỉ log ra đây. Bạn cần viết thêm API update điểm bên backend và repository.

        val currentTotal = (viewModel.diamondCount.value ?: 0)
        Log.d("SlotMachine", "User won $addedPoints points. New Total: $currentTotal")

        // TODO: Gọi API lưu điểm mới lên server tại đây để tránh mất điểm khi thoát app
        // Ví dụ: userRepository.updateUserPoints(token, userId, currentTotal) { ... }
    }

    override fun initListener() {
        binding.slotMachine.setOnImageClickListener {
            val currentSpins = viewModel.spinsLeft.value ?: 0
            if (isSpinning) return@setOnImageClickListener
            if (currentSpins > 0) {
                binding.slotMachine.setValueRandom(5, 15)
            }
        }
        binding.btnspin.singleClick {
            if (isSpinning) return@singleClick

            val spins = viewModel.spinsLeft.value ?: 0

            if (spins > 0) {
                binding.slotMachine.setValueRandom(5, 15)
            } else {
                Toast.makeText(context, "Not enough spins", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ivBack.singleClick {
            handleBack()
        }

        onBackPressed {
            handleBack()
        }
    }

    private fun handleBack() {
        val currentSpins = viewModel.spinsLeft.value ?: 0
        val currentDiamond = viewModel.diamondCount.value ?: 0

        context?.let {
            AppSharePref(it).spinCount = currentSpins
            // AppSharePref(it).diamondCount = currentDiamond // Không cần lưu local đè nữa nếu đã dùng API
        }

        sharedViewModel.isGoMystery = false
        popBackStack()
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateSpinUI(spinsLeft: Int) {
        binding.tvSpinsLeft.text = getString(R.string.spin_left, spinsLeft)
        binding.btnspin.isEnabled = !isSpinning
    }

    private fun showReward(data: SlotData) {
        context?.let { ctx ->
            val dialog = Dialog(ctx)
            dialog.setContentView(R.layout.dialog_collect)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawableResource(hoang.dqm.codebase.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val imvCenter = dialog.findViewById<AppCompatImageView>(R.id.imvCenter)
            val btnClose = dialog.findViewById<AppCompatButton>(R.id.btnClose)

            imvCenter.setImageResource(data.resDrawable)

            // Xử lý phần thưởng
            var pointsWon = 0
            when (data.resDrawable) {
                R.drawable.ic_500 -> {
                    pointsWon = 500 // Sửa lại logic điểm cho khớp với hình (vd hình 500 thì cộng 500)
                    viewModel.incrementDiamondCount(pointsWon)
                }
                R.drawable.ic_1000 -> {
                    pointsWon = 1000
                    viewModel.incrementDiamondCount(pointsWon)
                }
                R.drawable.ic_2000 -> {
                    pointsWon = 2000
                    viewModel.incrementDiamondCount(pointsWon)
                }
                R.drawable.ic_5000 -> {
                    // Nếu là spin thì không cộng điểm
                    viewModel.increaseSpinsLeft()
                }
            }

            // Nếu có điểm thưởng, gọi hàm lưu (Placeholder)
            if (pointsWon > 0) {
                savePointsToApi(pointsWon)
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    override fun initData() {
    }
}