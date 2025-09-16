package com.pbl6.fitme.toolbar

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pbl6.fitme.R
import com.pbl6.fitme.untils.singleClick
import android.widget.LinearLayout
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

        home.singleClick {
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
