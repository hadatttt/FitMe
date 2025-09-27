package com.pbl6.fitme.untils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.Toast
import androidx.core.animation.doOnEnd

/**
 * Extension function: click không delay
 */
fun <T : View> T.click(action: (T) -> Unit) {
    setOnClickListener {
        action(this)
    }
}

/**
 * Extension function: singleClick (chống double click)
 */
fun <T : View> T.singleClick(interval: Long = 300L, action: ((T) -> Unit)?) {
    setOnClickListener(SingleClickListener(interval, action))
}

/**
 * Class SingleClickListener: giới hạn click trong khoảng interval
 */
class SingleClickListener<T : View>(
    private val interval: Long = 300L,
    private var clickFunc: ((T) -> Unit)?
) : View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastClickTime > interval) {
            // chạy hiệu ứng scale khi click
            v.scaleAnimation {
                clickFunc?.invoke(v as T)
            }
            lastClickTime = nowTime
        }
    }
}

/**
 * Hiệu ứng scale khi click
 */
fun View.scaleAnimation(
    duration: Long = 200,
    scaleFactor: Float = 1.1f,
    onAnimationEnd: (() -> Unit)? = null
) {
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, scaleFactor, 1f)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, scaleFactor, 1f)

    val animatorSet = AnimatorSet().apply {
        playTogether(scaleX, scaleY)
        this.duration = duration
    }
    animatorSet.start()
    animatorSet.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            onAnimationEnd?.invoke()
        }
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    })
}
