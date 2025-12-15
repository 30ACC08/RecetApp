package com.example.recetapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.data.model.Review
import com.example.recetapp.databinding.ItemUserReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale

class UserReviewsAdapter(
    private val onRecipeClick: (Review) -> Unit,
    private val onEditClick: ((Review) -> Unit)? = null,   // Puede ser nulo
    private val onDeleteClick: ((Review) -> Unit)? = null, // Puede ser nulo
    private val isEditable: Boolean = false                // Controla la visibilidad
) : ListAdapter<Review, UserReviewsAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemUserReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(private val binding: ItemUserReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.tvRecipeName.text = review.recipeName
            binding.ratingBar.rating = review.rating
            binding.tvComment.text = review.comment

            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(review.timestamp)

            // Cargar imagen
            Glide.with(itemView)
                .load(review.recipeImageUrl)
                .centerCrop()
                .into(binding.ivRecipeThumbnail)

            // Lógica de visualización de botones
            if (isEditable) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE

                binding.btnEdit.setOnClickListener { onEditClick?.invoke(review) }
                binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(review) }
            } else {
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            }

            // Click en toda la tarjeta para ir a la receta
            binding.root.setOnClickListener { onRecipeClick(review) }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean = oldItem == newItem
    }
}