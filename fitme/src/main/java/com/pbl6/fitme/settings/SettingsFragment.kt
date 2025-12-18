package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentSettingsBinding
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideToolbar()
        initListeners()
    }

    private fun initListeners() {
        // Close Button
        binding.ivBack.singleClick {
            popBackStack()
        }

        // Profile
        binding.itemProfile.singleClick {
            navigate(R.id.SettingProfileFragment)
        }

        // Shipping Address
        binding.itemShipping.singleClick {
            navigate(R.id.shippingAddressFragment)
        }

        // Change Password
        binding.itemChangePassword.singleClick {
//            // Thay thế 'changePasswordFragment' bằng ID thực tế trong nav_graph của bạn
//            navigate(R.id.changePasswordFragment)
        }

        // Logout
        binding.itemLogout.singleClick {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        // Inflate custom layout cho dialog
        val dlgView = layoutInflater.inflate(R.layout.dialog_logout, null)

        val dlg = android.app.AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .create()

        // Làm tròn góc cho dialog nếu muốn đẹp hơn
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dlgView.findViewById<Button>(R.id.btnCancel)
        val btnLogout = dlgView.findViewById<Button>(R.id.btnLogout)

        btnCancel.setOnClickListener { dlg.dismiss() }

        btnLogout.setOnClickListener {
            // Xử lý clear session
            SessionManager.getInstance().clearSession(requireContext())

            dlg.dismiss()
            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()

            // Điều hướng về màn hình Splash hoặc Login
            navigate(R.id.splashFragment)
        }

        dlg.show()
    }

    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        if (toolbar != null) {
            toolbar.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}