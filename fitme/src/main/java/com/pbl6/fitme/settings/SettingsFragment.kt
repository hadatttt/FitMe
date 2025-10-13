package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentSettingsBinding
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
        val allItems = listOf(
            // Personal
            SettingOption("Profile"),
            SettingOption("Shipping Address"),
            SettingOption("Payment methods"),

            // Shop
            SettingOption("Country", "Vietnam", SettingType.TITLE_WITH_VALUE),
            SettingOption("Currency", "$ USD", SettingType.TITLE_WITH_VALUE),
            SettingOption("Sizes", "UK", SettingType.TITLE_WITH_VALUE),
            SettingOption("Terms and Conditions"),

            // Account
            SettingOption("Language", "English", SettingType.TITLE_WITH_VALUE),
            SettingOption("About ESHOP"),
            SettingOption("Logout")
        )

        binding.recyclerSettings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SettingsAdapter(allItems) { item ->
                handleItemClick(item)
            }
        }
        binding.ivClose.singleClick {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun handleItemClick(item: SettingOption) {
        when (item.title) {
            "Profile" -> navigate(R.id.profileFragment)
//            "Shipping Address" -> navigate(R.id.action_settings_to_shipping)
//            "Payment methods" -> navigate(R.id.action_settings_to_payment)
//            "Terms and Conditions" -> navigate(R.id.action_settings_to_terms)
//            "About Slada" -> navigate(R.id.action_settings_to_about)
            "Logout" -> showLogoutDialog()
            else -> Toast.makeText(requireContext(), "Clicked: ${item.title}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showLogoutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // TODO: Handle logout logic, e.g. clear token, navigate to Login
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                navigate(R.id.loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun hideToolbar() {
        val toolbar = requireActivity().findViewById<View>(R.id.toolbar)
        toolbar.visibility = View.GONE
    }
}
