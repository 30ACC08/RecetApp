package com.example.recetapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.data.model.RecipeTranslations
import com.example.recetapp.databinding.ItemRecipeBinding
import com.example.recetapp.databinding.ItemRecipeCompactBinding
import com.google.firebase.auth.FirebaseAuth

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit = {},
    private val onUserClick: (String) -> Unit = {}, // <--- Callback para ir al perfil
    private val isMyRecipesMode: Boolean = false
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onRecipeClick, onFavoriteClick, onUserClick, isMyRecipesMode)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        private val binding: ItemRecipeBinding,
        private val onRecipeClick: (Recipe) -> Unit,
        private val onFavoriteClick: (Recipe) -> Unit,
        private val onUserClick: (String) -> Unit,
        private val isMyRecipesMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {
            val context = binding.root.context
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            binding.apply {
                tvRecipeName.text = recipe.name

                val catEsp = RecipeTranslations.categoryName(recipe.category)
                val areaEsp = RecipeTranslations.areaName(recipe.area)
                tvRecipeCategory.text = "$catEsp • $areaEsp"

                Glide.with(context).load(recipe.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background).centerCrop().into(ivRecipeImage)

                // LÓGICA DE FUENTE Y CLIC EN USUARIO
                if (recipe.source == RecipeSource.USER) {
                    val autor = when {
                        recipe.userId == currentUserId -> "Mí"
                        recipe.creatorName.isNotBlank() -> recipe.creatorName
                        else -> "Anónimo"
                    }
                    tvSource.text = "Por: $autor"
                    tvSource.setBackgroundColor(context.getColor(R.color.teal_700))

                    // Solo activamos el click si no soy yo y hay un ID válido
                    if (recipe.userId.isNotBlank() && recipe.userId != currentUserId) {
                        tvSource.setOnClickListener { onUserClick(recipe.userId) }
                    } else {
                        tvSource.setOnClickListener(null)
                    }
                } else {
                    tvSource.text = if (recipe.source == RecipeSource.THEMEALDB) "MealDB" else "Spoonacular"
                    tvSource.setBackgroundColor(context.getColor(
                        if (recipe.source == RecipeSource.THEMEALDB) R.color.orange_primary else R.color.purple_500
                    ))
                    tvSource.setOnClickListener(null)
                }

                // TIEMPO
                tvTime.visibility = if (recipe.readyInMinutes != null) View.VISIBLE else View.GONE
                tvTime.text = "⏱ ${recipe.readyInMinutes} min"

                // CONTADOR DE LIKES (REAL)
                if (recipe.likesCount > 0) {
                    tvLikesCount.visibility = View.VISIBLE
                    tvLikesCount.text = recipe.likesCount.toString()
                } else {
                    tvLikesCount.visibility = View.GONE
                }

                // BOTÓN FAVORITO
                if (isMyRecipesMode) {
                    btnFavorite.setImageResource(android.R.drawable.ic_menu_delete)
                    btnFavorite.setColorFilter(context.getColor(R.color.error))
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_favorite)
                    btnFavorite.setColorFilter(context.getColor(R.color.error))
                }

                root.setOnClickListener { onRecipeClick(recipe) }
                btnFavorite.setOnClickListener { onFavoriteClick(recipe) }
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe) = oldItem == newItem
    }
}

class RecipeCompactAdapter(private val onRecipeClick: (Recipe) -> Unit) :
    ListAdapter<Recipe, RecipeCompactAdapter.RecipeViewHolder>(RecipeAdapter.RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onRecipeClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) { holder.bind(getItem(position)) }

    class RecipeViewHolder(private val binding: ItemRecipeCompactBinding, private val onRecipeClick: (Recipe) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.tvRecipeName.text = recipe.name
            Glide.with(binding.root).load(recipe.thumbnailUrl).centerCrop().into(binding.ivRecipeImage)
            binding.root.setOnClickListener { onRecipeClick(recipe) }
        }
    }
}