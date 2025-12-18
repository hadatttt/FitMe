package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pbl6.fitme.databinding.FragmentContactInforBinding
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

class ContactInforFragment : Fragment() {
    private var _binding: FragmentContactInforBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentContactInforBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load existing values: recipient name, phone, email
        val session = com.pbl6.fitme.session.SessionManager.getInstance()
        val recipient = session.getRecipientName(requireContext()) ?: ""
        val phone = session.getRecipientPhone(requireContext()) ?: ""
        val email = session.getUserEmail(requireContext()) ?: ""

        binding.etRecipientName.setText(recipient)
        binding.etCity.setText(phone)
        // Show email but do not allow editing here
        binding.etAddress.setText(email)
        binding.etAddress.isEnabled = false
        binding.ivBack.singleClick {
            popBackStack()
        }
        binding.btnSave.setOnClickListener {
            val newRecipient = binding.etRecipientName.text?.toString()?.trim() ?: ""
            val newPhone = binding.etCity.text?.toString()?.trim() ?: ""

            if (newRecipient.isNotBlank()) {
                session.saveRecipientName(requireContext(), newRecipient)
            }
            // allow empty phone but still save trimmed value
            session.saveRecipientPhone(requireContext(), newPhone)

            popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}