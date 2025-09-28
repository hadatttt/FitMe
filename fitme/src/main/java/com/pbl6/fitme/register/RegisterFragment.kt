package com.pbl6.fitme.register

import com.pbl6.fitme.databinding.FragmentRegisterBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.utils.singleClick

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterViewModel>() {


    override fun initView() {
    }

    override fun initListener() {
        binding.ivUpload.singleClick {

        }
    }

    override fun initData() {
    }

}
