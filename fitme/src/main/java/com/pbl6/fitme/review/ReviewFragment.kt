package com.pbl6.fitme.review

import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide // Đảm bảo project đã có thư viện Glide
import com.google.android.material.chip.Chip
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.FragmentReviewBinding
import com.pbl6.fitme.session.SessionManager
import hoang.dqm.codebase.base.activity.BaseFragment

class ReviewFragment : BaseFragment<FragmentReviewBinding, ReviewViewModel>() {

    private var productId: String? = null
    private var productName: String? = null
    private var productImage: String? = null

    private var currentUserEmail: String? = null

    override fun initView() {
        updateRatingStatus(binding.ratingBar.rating)
    }

    override fun initData() {
        currentUserEmail = SessionManager.getInstance()
            .getUserEmail(requireContext())

        val token = SessionManager.getInstance()
            .getAccessToken(requireContext()) ?: return

        arguments?.let {
            productId = it.getString("variant_id")
            productName = it.getString("product_name")
            productImage = it.getString("product_image_url")
        }

        productId?.let { variantId ->
            viewModel.getProductIdByVariantId(token, variantId) { pId ->
                if (pId != null) {
                    productId = pId
                }
            }
        }

        binding.tvProductNameReview.text = productName ?: "Unknown Product"

        if (!productImage.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(productImage)
                .placeholder(R.drawable.bg_circle_blue)
                .error(R.drawable.bg_circle_blue)
                .into(binding.ivProductThumb)
        }
    }


    override fun initListener() {
        // 1. Nút Back
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 2. RatingBar thay đổi
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            updateRatingStatus(rating)
        }

        // 3. Xử lý Quick Feedback Tags (ChipGroup)
        val chipGroup = binding.chipGroupFeedback
        for (i in 0 until chipGroup.childCount) {
            val view = chipGroup.getChildAt(i)
            if (view is Chip) {
                view.setOnClickListener {
                    addTagToComment(view.text.toString())
                }
            }
        }

        // 4. Nút Submit
        binding.btnSubmitReview.setOnClickListener {
            validateAndSubmit()
        }

        // 5. Quan sát kết quả từ ViewModel
        viewModel.createReviewResult.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to submit review. Try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Quan sát loading (nếu muốn hiện ProgressBar)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSubmitReview.isEnabled = !isLoading
            binding.btnSubmitReview.text = if(isLoading) "Submitting..." else "Submit Review"
        }
    }

    private fun updateRatingStatus(rating: Float) {
        val status = when (rating.toInt()) {
            5 -> "Excellent"
            4 -> "Good"
            3 -> "Average"
            2 -> "Poor"
            else -> "Terrible"
        }
        binding.tvRatingStatus.text = status
    }

    private fun addTagToComment(tag: String) {
        val currentText = binding.etComment.text.toString()
        if (!currentText.contains(tag)) {
            val newText = if (currentText.isBlank()) {
                tag
            } else {
                "$currentText, $tag"
            }
            binding.etComment.setText(newText)
            binding.etComment.setSelection(newText.length)
        }
    }
    private fun validateAndSubmit() {
        val rating = binding.ratingBar.rating.toInt()
        val comment = binding.etComment.text.toString().trim()
        val pId = productId

        if (pId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Product info missing", Toast.LENGTH_SHORT).show()
            return
        }

        if (comment.isEmpty()) {
            binding.tilComment.error = "Please write a comment"
            return
        } else {
            binding.tilComment.error = null
        }

        viewModel.submitReview(currentUserEmail, pId, rating, comment)
    }

}