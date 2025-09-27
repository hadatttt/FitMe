package com.pbl6.fitme.toolbar

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.untils.singleClick
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.pbl6.fitme.cart.CartFragment
import com.pbl6.fitme.profile.ProfileFragment
import com.pbl6.fitme.wishlist.WishlistFragment

open class ToolBarFragment : Fragment() {

    protected fun setupBottomNavigation(view: View) {
        val home: LinearLayout = view.findViewById(R.id.home_id)
        val wish: LinearLayout = view.findViewById(R.id.wish_id)
        val filter: LinearLayout = view.findViewById(R.id.filter_id)
        val cart: LinearLayout = view.findViewById(R.id.cart_id)
        val profile: LinearLayout = view.findViewById(R.id.person_id)

        val homeIcon: ImageView = view.findViewById(R.id.ic_home)
        val wishIcon: ImageView = view.findViewById(R.id.ic_wish)
        val filterIcon: ImageView = view.findViewById(R.id.ic_filter)
        val cartIcon: ImageView = view.findViewById(R.id.ic_card)
        val profileIcon: ImageView = view.findViewById(R.id.ic_person)

        val icons = listOf(homeIcon, wishIcon, filterIcon, cartIcon, profileIcon)

        fun setActive(activeIcon: ImageView) {
            icons.forEach { icon ->
                val color = if (icon == activeIcon) {
                    ContextCompat.getColor(requireContext(), R.color.black)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.maincolor)
                }
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color))
            }
        }
        home.singleClick {
            setActive(homeIcon)
            if (!isCurrentFragment(ProfileFragment::class.java)) {
                if (isCurrentFragment(WishlistFragment::class.java)) {
                    findNavController().navigate(R.id.action_wishlist_to_profile)
                }
                else if (isCurrentFragment(CartFragment::class.java)) {
                    findNavController().navigate(R.id.action_cart_to_profile)
                }
                else {
                    findNavController().navigate(R.id.action_hello_to_profile)
                }
            }
        }
        wish.singleClick {
            setActive(wishIcon)
            if (!isCurrentFragment(WishlistFragment::class.java)) {
                if (isCurrentFragment(ProfileFragment::class.java)) {
                    findNavController().navigate(R.id.action_profile_to_wishlist)
                }
                else if (isCurrentFragment(CartFragment::class.java)) {
                    findNavController().navigate(R.id.action_cart_to_wishlist)
                }
            }
        }
        filter.singleClick {

        }
        cart.singleClick {
            if (!isCurrentFragment(CartFragment::class.java)) {
                if (isCurrentFragment(ProfileFragment::class.java)) {
                    findNavController().navigate(R.id.action_profile_to_cart)
                }
                else if (isCurrentFragment(WishlistFragment::class.java)) {
                    findNavController().navigate(R.id.action_wishlist_to_cart)
                }
            }
        }
        profile.singleClick {
        }
    }
    private fun isCurrentFragment(fragmentClass: Class<out Fragment>): Boolean {
        return this::class.java == fragmentClass
    }
}
