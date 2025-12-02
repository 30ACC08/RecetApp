package com.example.recetapp.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Review
import com.example.recetapp.databinding.ItemReviewBinding

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.tvUserName.text = review.userName
            binding.tvComment.text = review.comment
            binding.rbRating.rating = review.rating

            // Formatear fecha (ej: "Hace 2 horas")
            val niceDate = DateUtils.getRelativeTimeSpanString(
                review.timestamp.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.tvDate.text = niceDate

            // Cargar foto de usuario
            if (review.userPhotoUrl.isNotEmpty()) {
                Glide.with(binding.root)
                    .load(review.userPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivUserPhoto)
            } else {
                binding.ivUserPhoto.setImageResource(R.drawable.ic_person)
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Review, newItem: Review) = oldItem == newItem
    }
}