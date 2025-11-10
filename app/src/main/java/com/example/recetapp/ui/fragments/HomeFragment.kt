package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.databinding.FragmentHomeBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.adapters.RecipeCompactAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()

    private lateinit var featuredAdapter: RecipeCompactAdapter
    private lateinit var recommendedAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        setupObservers()

        // Cargar datos iniciales
        viewModel.loadFeaturedRecipes()
    }

    private fun setupRecyclerViews() {
        // RecyclerView de recetas destacadas (horizontal)
        featuredAdapter = RecipeCompactAdapter { recipe ->
            navigateToDetail(recipe)
        }

        // Verificar si existe rv_featured_recipes
        try {
            binding.rvFeaturedRecipes.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = featuredAdapter
            }
        } catch (e: Exception) {
            // Si no existe el RecyclerView, no hace nada
        }

        // RecyclerView de recetas recomendadas (grid)
        recommendedAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                navigateToDetail(recipe)
            },
            onFavoriteClick = { recipe ->
                Toast.makeText(context, "Agregado a favoritos: ${recipe.name}", Toast.LENGTH_SHORT).show()
            }
        )

        // Verificar si existe rv_recommended_recipes
        try {
            binding.rvRecommendedRecipes.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = recommendedAdapter
            }
        } catch (e: Exception) {
            // Si no existe el RecyclerView, no hace nada
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Barra de búsqueda
            try {
                cvBuscar.setOnClickListener {
                    findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
                }
            } catch (e: Exception) {
                // El elemento no existe en el layout
            }
        }
    }

    private fun setupObservers() {
        // Recetas destacadas
        viewModel.featuredRecipes.observe(viewLifecycleOwner) { recipes ->
            featuredAdapter.submitList(recipes)
            recommendedAdapter.submitList(recipes.take(6))
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            try {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                // ProgressBar no existe
            }
        }

        // Error handling
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToDetail(recipe: Recipe) {
        viewModel.setSelectedRecipe(recipe)
        findNavController().navigate(R.id.action_homeFragment_to_detalleFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}