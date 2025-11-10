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
import com.example.recetapp.databinding.ItemRecipeBinding
import com.example.recetapp.databinding.ItemRecipeCompactBinding

// ==================== Adaptador Principal (Lista Completa) ====================

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit = {}
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding, onRecipeClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        private val binding: ItemRecipeBinding,
        private val onRecipeClick: (Recipe) -> Unit,
        private val onFavoriteClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {
            binding.apply {
                // Nombre
                tvRecipeName.text = recipe.name

                // Categoría y área
                tvRecipeCategory.text = "${recipe.category} • ${recipe.area}"

                // Imagen
                Glide.with(root.context)
                    .load(recipe.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(ivRecipeImage)

                // Badge de fuente
                tvSource.text = when (recipe.source) {
                    RecipeSource.THEMEALDB -> "MealDB"
                    RecipeSource.SPOONACULAR -> "Spoonacular"
                }
                tvSource.setBackgroundColor(
                    root.context.getColor(
                        if (recipe.source == RecipeSource.THEMEALDB)
                            R.color.orange_primary
                        else
                            R.color.purple_500
                    )
                )

                // Tiempo (si está disponible)
                if (recipe.readyInMinutes != null) {
                    tvTime.visibility = View.VISIBLE
                    tvTime.text = "⏱ ${recipe.readyInMinutes} min"
                } else {
                    tvTime.visibility = View.GONE
                }

                // Porciones (si está disponible)
                if (recipe.servings != null) {
                    tvServings.visibility = View.VISIBLE
                    tvServings.text = "👥 ${recipe.servings}"
                } else {
                    tvServings.visibility = View.GONE
                }

                // Health Score (si está disponible)
                if (recipe.healthScore != null) {
                    tvHealthScore.visibility = View.VISIBLE
                    tvHealthScore.text = "❤️ ${recipe.healthScore.toInt()}"
                } else {
                    tvHealthScore.visibility = View.GONE
                }

                // Tags dietéticos
                val dietTags = mutableListOf<String>()
                if (recipe.vegetarian) dietTags.add("🥬 Vegetariano")
                if (recipe.vegan) dietTags.add("🌱 Vegano")
                if (recipe.glutenFree) dietTags.add("🌾 Sin Gluten")
                if (recipe.dairyFree) dietTags.add("🥛 Sin Lácteos")

                if (dietTags.isNotEmpty()) {
                    tvDietTags.visibility = View.VISIBLE
                    tvDietTags.text = dietTags.joinToString(" • ")
                } else {
                    tvDietTags.visibility = View.GONE
                }

                // Calorías
                if (recipe.nutrition?.calories != null) {
                    tvCalories.visibility = View.VISIBLE
                    tvCalories.text = "${recipe.nutrition.calories.toInt()} kcal"
                } else {
                    tvCalories.visibility = View.GONE
                }

                // Click listeners
                root.setOnClickListener { onRecipeClick(recipe) }
                btnFavorite.setOnClickListener { onFavoriteClick(recipe) }
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}

// ==================== Adaptador Compacto (para carruseles) ====================

class RecipeCompactAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeCompactAdapter.RecipeViewHolder>(RecipeAdapter.RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeCompactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding, onRecipeClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        private val binding: ItemRecipeCompactBinding,
        private val onRecipeClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {
            binding.apply {
                tvRecipeName.text = recipe.name

                Glide.with(root.context)
                    .load(recipe.thumbnailUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(ivRecipeImage)

                if (recipe.readyInMinutes != null) {
                    tvTime.visibility = View.VISIBLE
                    tvTime.text = "${recipe.readyInMinutes} min"
                } else {
                    tvTime.visibility = View.GONE
                }

                root.setOnClickListener { onRecipeClick(recipe) }
            }
        }
    }
}