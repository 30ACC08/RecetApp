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
import com.example.recetapp.data.model.User
import com.example.recetapp.databinding.FragmentHomeBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.adapters.RecipeCompactAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.example.recetapp.ui.viewmodel.UiState

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

        // Carga inicial solo si no hay datos
        if (viewModel.homeState.value !is UiState.Success) {
            viewModel.loadHomeContent()
        }
    }

    private fun setupRecyclerViews() {
        featuredAdapter = RecipeCompactAdapter { navigateToDetail(it) }
        binding.rvFeaturedRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
        }

        breakfastAdapter = RecipeCompactAdapter { navigateToDetail(it) }
        binding.rvBreakfastRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = breakfastAdapter
        }

        healthyAdapter = RecipeAdapter(
            onRecipeClick = { navigateToDetail(it) },
            onFavoriteClick = {
                viewModel.toggleFavorite(it)
                Toast.makeText(context, "Actualizando favoritos...", Toast.LENGTH_SHORT).show()
            },
            onUserClick = { userId ->
                val user = User(id = userId)
                val bundle = Bundle().apply { putParcelable("user", user) }
                findNavController().navigate(R.id.action_global_publicProfileFragment, bundle)
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
        viewModel.homeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE

                    featuredAdapter.submitList(state.data.featured)
                    breakfastAdapter.submitList(state.data.breakfast)
                    healthyAdapter.submitList(state.data.healthy)
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
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