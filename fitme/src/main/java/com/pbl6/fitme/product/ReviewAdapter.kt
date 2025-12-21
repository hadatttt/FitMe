package com.pbl6.fitme.product

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.pbl6.fitme.R
import com.pbl6.fitme.databinding.ItemReviewBinding
import com.pbl6.fitme.model.Review
import com.pbl6.fitme.network.UserResult // Model User trả về từ API
import com.pbl6.fitme.repository.UserRepository
import com.pbl6.fitme.session.SessionManager
import com.pbl6.fitme.untils.AppConstrain
import com.pbl6.fitme.untils.AppSharePref

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var reviews: List<Review> = emptyList()

    // 1. Khởi tạo Repository bên trong Adapter (hoặc truyền qua constructor)
    private val userRepository = UserRepository()

    // 2. Cache để lưu thông tin User đã tải (Key: userId, Value: UserProfile)
    // Giúp danh sách mượt hơn, không phải load lại khi scroll
    private val userCache = mutableMapOf<String, UserResult>()

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
            val context = binding.root.context

            // --- Set thông tin Review cơ bản ---
            binding.rbReviewRating.rating = review.rating.toFloat()
            binding.tvReviewComment.text = review.comment

            // --- Xử lý hiển thị User (Tên + Avatar) ---
            val userId = review.userId

            binding.tvReviewUser.text = "Loading..."

            if (userId.isNullOrBlank()) {
                binding.tvReviewUser.text = "Anonymous"
                return
            }

            // KIỂM TRA CACHE
            if (userCache.containsKey(userId)) {
                // Nếu đã có trong cache -> Hiển thị ngay
                val cachedUser = userCache[userId]
                updateUserUI(cachedUser)
            } else {
                // Nếu chưa có -> Gọi API
                val token = SessionManager.getInstance().getAccessToken(context) ?: ""

                // Gọi API lấy thông tin user theo ID
                userRepository.getUserDetail(token, userId) { userProfile ->
                    // Vì callback này chạy ở background thread, cần post lên UI thread
                    binding.root.post {
                        if (userProfile != null) {
                            // Lưu vào cache
                            userCache[userId] = userProfile
                            if (userId == review.userId) {
                                updateUserUI(userProfile)
                            }
                        } else {
                            binding.tvReviewUser.text = "Unknown User"
                        }
                    }
                }
            }
        }

        // Trong hàm updateUserUI
        private fun updateUserUI(user: UserResult?) {
            if (user == null) return
            val context = binding.root.context

            binding.tvReviewUser.text = user.fullName ?: user.email ?: "User"

            val avatarUrl = user.avatarUrl
            if (avatarUrl.isNullOrEmpty()) {
                binding.ivUserAvatar.setImageResource(R.drawable.image)
                return
            }

            // 1. Token
            val token = SessionManager.getInstance().getAccessToken(context) ?: ""

            val baseUrl = "http://10.48.170.90:8080/api"

            // 3. Chuẩn hóa path
            val fullPath = if (avatarUrl.startsWith("/")) avatarUrl else "/$avatarUrl"
            val stringUrl = "$baseUrl$fullPath"

            // 4. GlideUrl có Authorization header
            val glideUrl = GlideUrl(
                stringUrl,
                LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            )

            // 5. Load ảnh
            Glide.with(context)
                .load(glideUrl)
                .placeholder(R.drawable.image)
                .error(R.drawable.image)
                .circleCrop()
                .into(binding.ivUserAvatar)
        }

    }
}