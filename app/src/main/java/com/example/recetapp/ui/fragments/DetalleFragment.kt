package com.example.recetapp.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.databinding.FragmentDetalleBinding
import com.example.recetapp.ui.viewmodel.RecipeViewModel

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
                val isFavNow = !(viewModel.isRecipeFavorite.value ?: false)
                Toast.makeText(context, if (isFavNow) "Guardado" else "Eliminado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeRecipe() {
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe == null) return@observe
            viewModel.checkIfFavorite(recipe.id)

            binding.tvRecipeName.text = recipe.name
            binding.tvCategory.text = recipe.category
            binding.tvArea.text = recipe.area
            Glide.with(this).load(recipe.imageUrl).centerCrop().into(binding.ivRecipeImage)

            // Renderizado de datos básicos
            val sourceName = if (recipe.source == RecipeSource.THEMEALDB) "MealDB" else "Spoonacular"
            binding.tvSource.text = sourceName
            binding.tvIngredientes.text = recipe.ingredients.joinToString("\n") { "• ${it.measure} ${it.name}" }
            binding.tvPreparacion.text = recipe.instructions
        }

        viewModel.isRecipeFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.ivFavorito.isSelected = isFav
            val color = if (isFav) R.color.error else R.color.text_secondary
            binding.ivFavorito.setColorFilter(resources.getColor(color, null))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}