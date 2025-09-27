package com.pbl6.fitme.login

import android.text.InputType
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentLoginBinding
import com.pbl6.fitme.untils.singleClick
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewmodel>() {

    private var isPasswordVisible = false

    override fun initView() {

    }

    override fun initListener() {
        binding.tvCancel.singleClick {
            navigate(R.id.cartFragment)
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
            navigate(R.id.cartFragment)
        }
    }


    override fun initData() {
        TODO("Not yet implemented")
    }
}