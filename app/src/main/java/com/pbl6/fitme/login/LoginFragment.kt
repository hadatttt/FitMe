package com.pbl6.fitme.login

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentLoginBinding
import com.pbl6.fitme.untils.singleClick

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding

    // trạng thái hiển thị mật khẩu
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.let { b ->
            // nút Cancel
            b.tvCancel.singleClick {
                val options = NavOptions.Builder()
                    .setEnterAnim(R.anim.inright)
                    .setExitAnim(R.anim.outleft)
                    .setPopEnterAnim(R.anim.inleft)
                    .setPopExitAnim(R.anim.outright)
                    .build()
                findNavController().navigate(R.id.action_login_to_slash, null, options)
            }

            // toggle ẩn/hiện mật khẩu
            b.ivTogglePassword.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    b.etPassword.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    b.ivTogglePassword.setImageResource(R.drawable.eye_slash) // icon mắt mở
                } else {
                    b.etPassword.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    b.ivTogglePassword.setImageResource(R.drawable.eye)
                }
                b.etPassword.setSelection(b.etPassword.text?.length ?: 0)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
