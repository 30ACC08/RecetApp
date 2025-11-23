package com.example.recetapp.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.RecipeFilter
import com.example.recetapp.databinding.FragmentSearchBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.google.android.material.chip.Chip

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        setupObservers()
        binding.etSearch.requestFocus()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                viewModel.setSelectedRecipe(recipe)
                findNavController().navigate(R.id.action_searchFragment_to_detalleFragment)
            },
            onFavoriteClick = { recipe ->
                viewModel.toggleFavorite(recipe)
                Toast.makeText(context, "Actualizando favoritos...", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) viewModel.searchRecipesDebounced(query)
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchRecipes(binding.etSearch.text.toString())
                binding.etSearch.clearFocus()
                true
            } else false
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // FILTROS AVANZADOS (Botón)
        binding.btnFilters.setOnClickListener { showFilterDialog() }

        binding.btnSearch.setOnClickListener { viewModel.searchRecipes(binding.etSearch.text.toString()) }
        binding.tvClearFilters.setOnClickListener { viewModel.clearFilters(); binding.etSearch.setText("") }

        // FILTROS BÁSICOS (Chips)
        binding.chipDesayuno.setOnClickListener { viewModel.toggleCategory("Breakfast") }
        binding.chipComida.setOnClickListener { viewModel.toggleCategory("Beef") }
        binding.chipCena.setOnClickListener { viewModel.toggleCategory("Seafood") }
        binding.chipPostres.setOnClickListener { viewModel.toggleCategory("Dessert") }
        binding.chipVegetariano.setOnClickListener { viewModel.toggleVegetarian() }
        binding.chipVegano.setOnClickListener { viewModel.toggleVegan() }
    }

    private fun setupObservers() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
            val count = recipes.size
            binding.tvResultsCount.text = if (count == 0) "Sin resultados" else "$count recetas"
            binding.tvEmptyState.visibility = if (count == 0 && !binding.progressBar.isShown) View.VISIBLE else View.GONE
            binding.rvSearchResults.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) binding.tvEmptyState.visibility = View.GONE
        }

        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateChipsUI(filter)
            val hasFilters = (filter.category != null || filter.vegetarian == true || filter.vegan == true)
            binding.tvClearFilters.visibility = if (hasFilters) View.VISIBLE else View.GONE
            binding.llActiveFilters.visibility = if (hasFilters) View.VISIBLE else View.GONE
        }
    }

    private fun updateChipsUI(filter: RecipeFilter) {
        fun updateChip(chip: Chip, isActive: Boolean) {
            val color = if (isActive) R.color.orange_primary else R.color.background_gray
            val textColor = if (isActive) R.color.white else R.color.text_primary
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
            chip.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        }
        updateChip(binding.chipDesayuno, filter.category == "Breakfast")
        updateChip(binding.chipComida, filter.category == "Beef")
        updateChip(binding.chipCena, filter.category == "Seafood")
        updateChip(binding.chipPostres, filter.category == "Dessert")
        updateChip(binding.chipVegetariano, filter.vegetarian == true)
        updateChip(binding.chipVegano, filter.vegan == true)
    }

    private fun showFilterDialog() {
        val current = viewModel.currentFilter.value ?: RecipeFilter()
        FilterBottomSheetFragment.newInstance(current) { filter ->
            viewModel.applyFilter(filter)
        }.show(parentFragmentManager, "filter")
    }
}