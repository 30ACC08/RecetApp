package com.example.recetapp.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.databinding.FragmentDetalleBinding
import com.example.recetapp.ui.viewmodel.RecipeViewModel

class DetalleFragment : Fragment() {

    private var _binding: FragmentDetalleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()
    private var esFavorito = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeRecipe()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivFavorito.setOnClickListener {
            esFavorito = !esFavorito
            binding.ivFavorito.isSelected = esFavorito
            val mensaje = if (esFavorito) "Agregado a favoritos" else "Eliminado de favoritos"
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }

        binding.btnComenzarCocinar.setOnClickListener {
            Toast.makeText(context, "Iniciando modo cocina...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRecipe() {
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe == null) {
                Toast.makeText(context, "Error al cargar receta", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@observe
            }

            binding.apply {
                // Nombre
                tvRecipeName.text = recipe.name

                // Categoría y área
                tvCategory.text = recipe.category
                tvArea.text = recipe.area

                // Imagen
                Glide.with(requireContext())
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
                    resources.getColor(
                        if (recipe.source == RecipeSource.THEMEALDB)
                            R.color.orange_primary
                        else
                            R.color.purple_500,
                        null
                    )
                )

                // Información básica
                val basicInfo = buildString {
                    if (recipe.readyInMinutes != null) {
                        append("⏱ ${recipe.readyInMinutes} min")
                    }
                    if (recipe.servings != null) {
                        if (isNotEmpty()) append("  •  ")
                        append("👥 ${recipe.servings} porciones")
                    }
                    if (recipe.healthScore != null) {
                        if (isNotEmpty()) append("  •  ")
                        append("❤️ ${recipe.healthScore.toInt()}")
                    }
                }
                tvBasicInfo.text = basicInfo.ifEmpty { "Información no disponible" }

                // Tags dietéticos
                val dietTags = mutableListOf<String>()
                if (recipe.vegetarian) dietTags.add("🥬 Vegetariano")
                if (recipe.vegan) dietTags.add("🌱 Vegano")
                if (recipe.glutenFree) dietTags.add("🌾 Sin Gluten")
                if (recipe.dairyFree) dietTags.add("🥛 Sin Lácteos")

                if (dietTags.isNotEmpty()) {
                    llDietTags.visibility = View.VISIBLE
                    tvDietTags.text = dietTags.joinToString(" • ")
                } else {
                    llDietTags.visibility = View.GONE
                }

                // Video de YouTube
                if (!recipe.videoUrl.isNullOrEmpty()) {
                    btnWatchVideo.visibility = View.VISIBLE
                    btnWatchVideo.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.videoUrl))
                        startActivity(intent)
                    }
                } else {
                    btnWatchVideo.visibility = View.GONE
                }

                // Ingredientes
                val ingredientes = if (recipe.ingredients.isNotEmpty()) {
                    recipe.ingredients.joinToString("\n") { "• ${it.measure} ${it.name}" }
                } else {
                    "No hay ingredientes disponibles"
                }
                tvIngredientes.text = ingredientes

                // Preparación
                tvPreparacion.text = recipe.instructions.ifEmpty { "No hay instrucciones disponibles" }

                // Información nutricional
                if (recipe.nutrition != null) {
                    llNutrition.visibility = View.VISIBLE
                    tvCalories.text = "${recipe.nutrition.calories.toInt()} kcal"
                    tvProtein.text = "Proteínas: ${recipe.nutrition.protein.toInt()}g"
                    tvFat.text = "Grasas: ${recipe.nutrition.fat.toInt()}g"
                    tvCarbs.text = "Carbohidratos: ${recipe.nutrition.carbs.toInt()}g"
                } else {
                    llNutrition.visibility = View.GONE
                }

                // Precio
                if (recipe.pricePerServing != null) {
                    llPrice.visibility = View.VISIBLE
                    tvPrice.text = "Precio estimado por porción: $${String.format("%.2f", recipe.pricePerServing / 100)}"
                } else {
                    llPrice.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}