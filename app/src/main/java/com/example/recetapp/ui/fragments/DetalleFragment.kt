package com.example.recetapp.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.data.model.RecipeTranslations
import com.example.recetapp.databinding.FragmentDetalleBinding
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth // <--- Importante

class DetalleFragment : Fragment() {

    private var _binding: FragmentDetalleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeRecipe()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.ivFavorito.setOnClickListener {
            viewModel.selectedRecipe.value?.let { recipe ->
                viewModel.toggleFavorite(recipe)
                val msg = if (viewModel.isRecipeFavorite.value == true) "Eliminado de favoritos" else "Añadido a favoritos"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTranslate.setOnClickListener {
            viewModel.toggleTranslation()
        }

        binding.btnComenzarCocinar.setOnClickListener {
            Toast.makeText(context, "¡A cocinar!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRecipe() {
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe == null) return@observe

            viewModel.checkIfFavorite(recipe.id)
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            binding.apply {
                tvRecipeName.text = recipe.name

                tvCategory.text = RecipeTranslations.categoryName(recipe.category)
                tvArea.text = RecipeTranslations.areaName(recipe.area)

                Glide.with(this@DetalleFragment).load(recipe.imageUrl).centerCrop().into(ivRecipeImage)

                // === CORRECCIÓN DE NOMBRE EN DETALLE ===
                if (recipe.source == RecipeSource.USER) {
                    val autor = when {
                        recipe.userId == currentUserId -> "Mí"
                        recipe.creatorName.isNotBlank() -> recipe.creatorName
                        else -> "Anónimo"
                    }
                    tvSource.text = "Por: $autor"
                    tvSource.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
                } else {
                    tvSource.text = if (recipe.source == RecipeSource.THEMEALDB) "MealDB" else "Spoonacular"
                    val colorRes = if (recipe.source == RecipeSource.THEMEALDB) R.color.orange_primary else R.color.purple_500
                    tvSource.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
                }
                tvSource.visibility = View.VISIBLE

                val info = StringBuilder()
                if (recipe.readyInMinutes != null) info.append("⏱ ${recipe.readyInMinutes} min  ")
                if (recipe.healthScore != null) info.append("❤️ ${recipe.healthScore.toInt()}")
                tvBasicInfo.text = info.toString()

                val ingList = recipe.ingredients.joinToString("\n") { "• ${it.measure} ${it.name}" }
                tvIngredientes.text = ingList.ifBlank { "Ver instrucciones para ingredientes" }

                tvPreparacion.text = recipe.instructions.ifBlank { "No hay instrucciones disponibles" }

                if (!recipe.videoUrl.isNullOrEmpty()) {
                    btnWatchVideo.visibility = View.VISIBLE
                    btnWatchVideo.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recipe.videoUrl)))
                    }
                } else {
                    btnWatchVideo.visibility = View.GONE
                }

                if (recipe.nutrition != null) {
                    llNutrition.visibility = View.VISIBLE
                    tvCalories.text = "${recipe.nutrition.calories.toInt()} kcal"
                    tvProtein.text = "Prot: ${recipe.nutrition.protein.toInt()}g"
                    tvFat.text = "Grasa: ${recipe.nutrition.fat.toInt()}g"
                    tvCarbs.text = "Carbs: ${recipe.nutrition.carbs.toInt()}g"
                } else {
                    llNutrition.visibility = View.GONE
                }

                if (recipe.pricePerServing != null) {
                    llPrice.visibility = View.VISIBLE
                    tvPrice.text = "Costo: $${String.format("%.2f", recipe.pricePerServing / 100)}"
                } else {
                    llPrice.visibility = View.GONE
                }
            }
        }

        viewModel.isTranslated.observe(viewLifecycleOwner) { translated ->
            binding.btnTranslate.text = if (translated) "Ver Original" else "Traducir"
        }

        viewModel.isRecipeFavorite.observe(viewLifecycleOwner) { isFav ->
            if (isFav == null) {
                binding.ivFavorito.isEnabled = false
                binding.ivFavorito.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_hint))
            } else {
                binding.ivFavorito.isEnabled = true
                binding.ivFavorito.isSelected = isFav
                val color = if (isFav) R.color.error else R.color.text_secondary
                binding.ivFavorito.setColorFilter(ContextCompat.getColor(requireContext(), color))
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}