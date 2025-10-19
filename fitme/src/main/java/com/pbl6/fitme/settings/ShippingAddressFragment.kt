package com.pbl6.fitme.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pbl6.fitme.databinding.FragmentShippingAddressBinding
import hoang.dqm.codebase.base.activity.popBackStack

class ShippingAddressFragment : Fragment() {
    private var _binding: FragmentShippingAddressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentShippingAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSelectCountry.setOnClickListener { /* TODO: show country picker */ }
        binding.btnSave.setOnClickListener { popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
