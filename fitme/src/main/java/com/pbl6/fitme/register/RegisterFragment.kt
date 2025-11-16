package com.pbl6.fitme.register

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.widget.Toast
import com.pbl6.fitme.R
import androidx.core.widget.addTextChangedListener
import hoang.dqm.codebase.base.activity.navigate
import com.pbl6.fitme.databinding.FragmentRegisterBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick
import androidx.activity.result.contract.ActivityResultContracts
import com.pbl6.fitme.network.RegisterRequest
import com.pbl6.fitme.repository.AuthRepository
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.popBackStack
import kotlin.toString

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterViewModel>() {
    private var editTextValue1: String = ""
    private val authRepository = AuthRepository()
    var isPasswordVisible = true

    override fun initView() {
        binding.etEmail.addTextChangedListener { s ->
            editTextValue1 = s.toString()
        }
    }

    override fun initListener() {
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
        binding.tvCancel.singleClick {
            popBackStack()
        }
        binding.btnDone.singleClick {
            handleRegistration()
        }
    }

    override fun initData() {
    }
//    private fun openCamera() {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        cameraLauncher.launch(intent)
//    }

    private fun handleRegistration() {
        // 1. Lấy dữ liệu từ các EditText
        val firstName = binding.etFirstname.text.toString().trim()
        val lastName = binding.etLastname.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // 2. Kiểm tra dữ liệu (Validation)
        if (firstName.isEmpty()) {
            binding.etFirstname.error = "First name is required"
            return
        }
        if (lastName.isEmpty()) {
            binding.etLastname.error = "Last name is required"
            return
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return
        }
        val fullName = "$firstName $lastName"

        val registerRequest = RegisterRequest(
            username = email, // Hoặc bạn có thể thêm một trường EditText cho username
            password = password,
            email = email,
            fullName = fullName,
            dateOfBirth = null,
            phone = null,
            avatarUrl = null,
            gender = null
        )
        authRepository.register(registerRequest) { response ->
            if (response != null ) {
                Toast.makeText(requireContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                navigate(R.id.loginFragment)
            } else {
                Log.d("Auth", "Register response: $response")
            }
        }
    }

}