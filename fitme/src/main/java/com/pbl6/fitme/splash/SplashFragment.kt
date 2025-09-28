package com.pbl6.fitme.splash

import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentSplashBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class SplashFragment : BaseFragment <FragmentSplashBinding, SplashViewModel>() {


    override fun initView() {
    }
    override fun initListener() {
        binding.ivRegister.singleClick {
            navigate(R.id.registerFragment)
        }
        binding.ivNextLogin.singleClick {
            navigate(R.id.loginFragment)
        }
    }
    override fun initData() {
    }
}


