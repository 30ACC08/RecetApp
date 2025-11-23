package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
    private val viewModel: RecipeViewModel by activityViewModels()

    private lateinit var featuredAdapter: RecipeCompactAdapter
    private lateinit var breakfastAdapter: RecipeCompactAdapter
    private lateinit var healthyAdapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        setupObservers()

        // Carga inicial completa
        if (viewModel.featuredRecipes.value.isNullOrEmpty()) {
            viewModel.loadHomeContent()
        }
    }

    private fun setupRecyclerViews() {
        // 1. Tendencias (Horizontal)
        featuredAdapter = RecipeCompactAdapter { navigateToDetail(it) }
        binding.rvFeaturedRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
        }

        // 2. Desayunos (Horizontal)
        breakfastAdapter = RecipeCompactAdapter { navigateToDetail(it) }
        binding.rvBreakfastRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = breakfastAdapter
        }

        // 3. Saludables (Vertical)
        healthyAdapter = RecipeAdapter(
            onRecipeClick = { navigateToDetail(it) },
            onFavoriteClick = {
                viewModel.toggleFavorite(it)
                Toast.makeText(context, "Actualizando favoritos...", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvHealthyRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = healthyAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.cvBuscar.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setupObservers() {
        viewModel.featuredRecipes.observe(viewLifecycleOwner) { featuredAdapter.submitList(it) }
        viewModel.breakfastRecipes.observe(viewLifecycleOwner) { breakfastAdapter.submitList(it) }
        viewModel.healthyRecipes.observe(viewLifecycleOwner) { healthyAdapter.submitList(it) }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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