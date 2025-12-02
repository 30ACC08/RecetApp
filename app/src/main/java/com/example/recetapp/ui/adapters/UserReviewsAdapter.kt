package com.example.recetapp.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Review
import com.example.recetapp.databinding.ItemUserReviewBinding

class UserReviewsAdapter(
    private val onRecipeClick: (Review) -> Unit,
    private val onEditClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit
) : ListAdapter<Review, UserReviewsAdapter.UserReviewViewHolder>(UserReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserReviewViewHolder {
        val binding = ItemUserReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserReviewViewHolder(binding, onRecipeClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: UserReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserReviewViewHolder(
        private val binding: ItemUserReviewBinding,
        private val onRecipeClick: (Review) -> Unit,
        private val onEdit: (Review) -> Unit,
        private val onDelete: (Review) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            // Asegúrate de que tu modelo Review.kt tenga recipeName y recipeImageUrl
            binding.tvRecipeTitle.text = review.recipeName
            binding.tvUserComment.text = review.comment
            binding.rbUserRating.rating = review.rating

            try {
                val date = DateUtils.getRelativeTimeSpanString(
                    review.timestamp.time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
                binding.tvReviewDate.text = date
            } catch (e: Exception) {
                binding.tvReviewDate.text = "Reciente"
            }

            Glide.with(binding.root)
                .load(review.recipeImageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivRecipeThumb)

            // Clic en la tarjeta -> Ir a receta
            binding.root.setOnClickListener {
                onRecipeClick(review)
            }

            // Menú opciones
            binding.btnMenu.setOnClickListener { view ->
                showPopupMenu(view, review)
            }
        }

        private fun showPopupMenu(view: View, review: Review) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add(0, 1, 0, "Editar")
            popup.menu.add(0, 2, 1, "Eliminar")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { onEdit(review); true }
                    2 -> { onDelete(review); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    // Clase para comparar listas y animar cambios
    class UserReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Review, newItem: Review) = oldItem == newItem
    }
}