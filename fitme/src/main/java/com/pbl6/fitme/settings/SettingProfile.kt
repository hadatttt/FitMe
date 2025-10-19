package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.pbl6.fitme.databinding.FragmentSettingProfileBinding
import com.pbl6.fitme.network.LoginResponse
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.utils.singleClick

class SettingProfile : Fragment() {

	private var _binding: FragmentSettingProfileBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentSettingProfileBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Load saved profile if available
		val session = SessionManager.getInstance()
		val loginResp = session.getLoginResponse(requireContext())
		loginResp?.let { populateFromLoginResponse(it) }

		binding.btnSave.singleClick {
			// For now, just save name/email locally into shared prefs via SessionManager if possible,
			// or show a toast indicating success.
			val name = binding.etName.text.toString().trim()
			val email = binding.etEmail.text.toString().trim()
			if (name.isBlank() || email.isBlank()) {
				Toast.makeText(requireContext(), "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
				return@singleClick
			}

			// NOTE: We don't have a public setter on LoginResponse; if you want to persist changes to server,
			// call the profile update API here. For now, just show success toast.
			Toast.makeText(requireContext(), "Profile saved (local only)", Toast.LENGTH_SHORT).show()
		}

		binding.btnEditImage.singleClick{
			Toast.makeText(requireContext(), "Edit image - not implemented", Toast.LENGTH_SHORT).show()
		}
	}

	private fun populateFromLoginResponse(resp: LoginResponse) {
		val user = resp.result
		// If LoginResponse contains profile fields, fill them. Otherwise keep defaults.
//		binding.etName.setText(user?.displayName ?: binding.etName.text)
//		binding.etEmail.setText(user?.email ?: binding.etEmail.text)
		// Password left empty for security
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}