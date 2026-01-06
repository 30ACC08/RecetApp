package com.example.recetapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Review
import com.example.recetapp.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale

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
            // Asignación de textos
            binding.tvUserName.text = review.userName
            binding.tvComment.text = review.comment

            // Rating (el ID en XML es rb_rating)
            binding.rbRating.rating = review.rating

            // Formato de fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(review.timestamp)

            // Carga de imagen (CORREGIDO: usa userPhotoUrl en vez de userImage)
            if (review.userPhotoUrl.isNotBlank()) {
                Glide.with(binding.root)
                    .load(review.userPhotoUrl)
                    .circleCrop()
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