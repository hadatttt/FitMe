package com.pbl6.fitme.login

import android.text.InputType
import android.util.Log
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentLoginBinding
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewmodel>() {

    private var isPasswordVisible = false

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    override fun initView() {

    }
    private val authRepository = com.pbl6.fitme.repository.AuthRepository()
    private val mainRepository = com.pbl6.fitme.repository.MainRepository()

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
            
            // Validate email format
            if (!isValidEmail(email)) {
                binding.etError.text = "Login Fail: Email is not valid"
                binding.etError.visibility = android.view.View.VISIBLE
                return@singleClick
            }
            
            // Validate password length
            if (!isValidPassword(password)) {
                binding.etError.text = "Login Fail: Password has at least 6 characters"
                binding.etError.visibility = android.view.View.VISIBLE
                return@singleClick
            }

            // Clear any previous error messages
            binding.etError.visibility = android.view.View.GONE
            
            authRepository.login(email, password) { response ->
                if (response != null && response.result?.token?.isNotEmpty() == true) {
                    SessionManager.getInstance().saveLoginResponse(requireContext(), response)
                    // Persist the entered email separately so code can access it even when
                    // the backend token payload doesn't include email.
                    SessionManager.getInstance().saveUserEmail(requireContext(), email)
                    android.util.Log.d("SessionManager", "LoginResponse saved: token=${response.result.token}")

                    // Try to fetch user profile by email to obtain userId and save it locally.
                    val token = response.result.token
                    mainRepository.fetchAndStoreUserId(token, email) { uid ->
                        activity?.runOnUiThread {
                            try {
                                android.util.Log.d("LoginFragment", "fetchAndStoreUserId callback: uid=$uid, email=$email")
                                if (!uid.isNullOrBlank()) {
                                    com.pbl6.fitme.session.SessionManager.getInstance().saveUserId(requireContext(), uid)
                                    android.util.Log.d("LoginFragment", "Saved userId=$uid into SessionManager")
                                } else {
                                    android.util.Log.w("LoginFragment", "UserId not returned for email=$email")
                                }
                            } catch (ex: Exception) {
                                android.util.Log.e("LoginFragment", "Failed saving userId", ex)
                            }
                            navigate(R.id.homeFragment)
                        }
                    }
                } else {
                    Log.d("Auth", "Login response: ${response}")
                    binding.etError.text = "Login Fail: Email or password is incorrect."
                    binding.etError.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun initData() {
    }
}