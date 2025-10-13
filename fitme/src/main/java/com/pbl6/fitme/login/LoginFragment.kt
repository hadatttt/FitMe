package com.pbl6.fitme.login

import android.text.InputType
import android.util.Log
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentLoginBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewmodel>() {

    private var isPasswordVisible = false

    override fun initView() {

    }
    private val authRepository = com.pbl6.fitme.repository.AuthRepository()
    // Truy cập MainRepository qua Activity hoặc Singleton
    private val mainRepository = com.pbl6.fitme.repository.MainRepository

    override fun initListener() {
        binding.tvCancel.singleClick {
            popBackStack()
        }
        binding.ivTogglePassword.singleClick {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.eye)
            } else {
                binding.etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.eye_slash)
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }
        binding.btnNext.singleClick {
            val email = binding.etEmail.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            authRepository.login(email, password) { response ->
                if (response != null && response.result?.token?.isNotEmpty() == true) {
                    // Truyền token cho MainRepository
                    mainRepository.setToken(response.result.token)
                    navigate(R.id.profileFragment)
                } else {
                    Log.d("Auth", "Login response: ${response}")
                    binding.etError.text = "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin."
                    binding.etError.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun initData() {
    }
}