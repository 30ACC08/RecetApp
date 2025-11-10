// Ruta: app/src/main/java/com/example/recetapp/ui/fragments/SearchFragment.kt
package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.RecipeFilter
import com.example.recetapp.databinding.FragmentSearchBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()

    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        setupObservers()

        // Cargar recetas si ya hay un filtro activo
        viewModel.currentFilter.value?.let {
            if (it.query.isNotBlank() || viewModel.hasActiveFilters()) {
                viewModel.searchRecipes(it.query)
            }
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                navigateToDetail(recipe)
            },
            onFavoriteClick = { recipe ->
                Toast.makeText(context, "Agregado a favoritos: ${recipe.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun setupSearchBar() {
        binding.apply {
            // Búsqueda al presionar enter
            etSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = etSearch.text.toString()
                    viewModel.searchRecipes(query)
                    etSearch.clearFocus()
                    true
                } else false
            }

            // Búsqueda en tiempo real (opcional)
            etSearch.addTextChangedListener { text ->
                // Puedes habilitar búsqueda en tiempo real descomentando esto:
                // viewModel.searchRecipes(text.toString())
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Botón atrás
            btnBack.setOnClickListener {
                findNavController().popBackStack()
            }

            // Botón de filtros
            btnFilters.setOnClickListener {
                showFilterDialog()
            }

            // Botón de búsqueda
            btnSearch.setOnClickListener {
                val query = etSearch.text.toString()
                viewModel.searchRecipes(query)
            }

            // Chips de categorías rápidas
            chipDesayuno.setOnClickListener {
                viewModel.searchByCategory("Breakfast")
            }

            chipComida.setOnClickListener {
                viewModel.searchByCategory("Beef")
            }

            chipCena.setOnClickListener {
                viewModel.searchByCategory("Seafood")
            }

            chipPostres.setOnClickListener {
                viewModel.searchByCategory("Dessert")
            }

            chipVegetariano.setOnClickListener {
                viewModel.updateFilter(vegetarian = true)
            }

            chipVegano.setOnClickListener {
                viewModel.updateFilter(vegan = true)
            }

            // Limpiar filtros
            tvClearFilters.setOnClickListener {
                viewModel.clearFilters()
                etSearch.setText("")
            }
        }
    }

    private fun setupObservers() {
        // Resultados de búsqueda
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)

            // Actualizar contador de resultados
            binding.tvResultsCount.text = if (recipes.isEmpty()) {
                "No se encontraron recetas"
            } else {
                "${recipes.size} recetas encontradas"
            }

            // Mostrar/ocultar vista vacía
            binding.tvEmptyState.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSearchResults.visibility = if (recipes.isEmpty()) View.GONE else View.VISIBLE
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSearch.isEnabled = !isLoading
        }

        // Error handling
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        // Filtros activos
        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterBadge(filter)
        }
    }

    private fun updateFilterBadge(filter: RecipeFilter) {
        val activeFilters = viewModel.getActiveFiltersCount()

        binding.apply {
            if (activeFilters > 0) {
                tvFilterBadge.visibility = View.VISIBLE
                tvFilterBadge.text = activeFilters.toString()
                tvActiveFilters.visibility = View.VISIBLE
                tvActiveFilters.text = "$activeFilters filtros activos"
                tvClearFilters.visibility = View.VISIBLE
            } else {
                tvFilterBadge.visibility = View.GONE
                tvActiveFilters.visibility = View.GONE
                tvClearFilters.visibility = View.GONE
            }
        }
    }

    private fun showFilterDialog() {
        val currentFilter = viewModel.currentFilter.value ?: RecipeFilter()

        val filterDialog = FilterBottomSheetFragment.newInstance(currentFilter) { filter ->
            viewModel.applyFilter(filter)
        }

        filterDialog.show(parentFragmentManager, "filter_dialog")
    }

    private fun navigateToDetail(recipe: Recipe) {
        viewModel.setSelectedRecipe(recipe)
        findNavController().navigate(R.id.action_searchFragment_to_detalleFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}