package com.pbl6.fitme.product


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.databinding.ItemReviewBinding // Đảm bảo import đúng binding
import com.pbl6.fitme.model.Review

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var reviews: List<Review> = emptyList()

    fun setList(newList: List<Review>) {
        this.reviews = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.rbReviewRating.rating = review.rating.toFloat()
            binding.tvReviewComment.text = review.comment

            // Model Review của bạn chỉ có 'userId', chưa có 'username'.
            // Tạm thời, chúng ta sẽ hiển thị "Anonymous" hoặc một phần của ID.
            // Nếu bạn có thể lấy username từ userId, hãy cập nhật logic ở đây.
            binding.tvReviewUser.text = "Anonymous User"
            // Hoặc: binding.tvReviewUser.text = "User ${review.userId.substring(0, 8)}..."
        }
    }
}