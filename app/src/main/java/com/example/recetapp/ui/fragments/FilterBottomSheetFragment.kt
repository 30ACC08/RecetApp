package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.recetapp.R
import com.example.recetapp.data.model.*
import com.example.recetapp.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var currentFilter: RecipeFilter = RecipeFilter()
    private var onFilterApplied: ((RecipeFilter) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupAreaSpinner()
        setupSourceChips()
        setupDietaryChips()
        setupSliders()
        setupListeners()
        loadCurrentFilter()
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Todas") + RecipeCategories.ALL
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupAreaSpinner() {
        val areas = listOf("Todas") + RecipeAreas.ALL
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, areas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerArea.adapter = adapter
    }

    private fun setupSourceChips() {
        binding.apply {
            chipSourceAll.isChecked = true

            chipGroupSource.setOnCheckedStateChangeListener { _, checkedIds ->
                // Permitir solo uno seleccionado
                if (checkedIds.isEmpty()) {
                    chipSourceAll.isChecked = true
                }
            }
        }
    }

    private fun setupDietaryChips() {
        // Las chips dietéticas pueden estar todas seleccionadas
    }

    private fun setupSliders() {
        binding.apply {
            // Slider de tiempo máximo
            sliderMaxTime.addOnChangeListener { _, value, _ ->
                tvMaxTimeValue.text = value.toInt().toString()
            }

            // Slider de health score mínimo
            sliderMinHealth.addOnChangeListener { _, value, _ ->
                tvMinHealthValue.text = value.toInt().toString()
            }

            // Slider de calorías máximas
            sliderMaxCalories.addOnChangeListener { _, value, _ ->
                tvMaxCaloriesValue.text = value.toInt().toString()
            }

            // Switches para habilitar/deshabilitar sliders
            switchMaxTime.setOnCheckedChangeListener { _, isChecked ->
                sliderMaxTime.isEnabled = isChecked
            }

            switchMinHealth.setOnCheckedChangeListener { _, isChecked ->
                sliderMinHealth.isEnabled = isChecked
            }

            switchMaxCalories.setOnCheckedChangeListener { _, isChecked ->
                sliderMaxCalories.isEnabled = isChecked
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnApplyFilters.setOnClickListener {
                applyFilters()
            }

            btnClearFilters.setOnClickListener {
                clearFilters()
            }
        }
    }

    private fun loadCurrentFilter() {
        binding.apply {
            // Categoría
            val categoryIndex = if (currentFilter.category != null) {
                RecipeCategories.ALL.indexOf(currentFilter.category) + 1
            } else 0
            spinnerCategory.setSelection(categoryIndex)

            // Área
            val areaIndex = if (currentFilter.area != null) {
                RecipeAreas.ALL.indexOf(currentFilter.area) + 1
            } else 0
            spinnerArea.setSelection(areaIndex)

            // Fuente
            when (currentFilter.source) {
                RecipeSource.THEMEALDB -> chipSourceMealdb.isChecked = true
                RecipeSource.SPOONACULAR -> chipSourceSpoonacular.isChecked = true
                null -> chipSourceAll.isChecked = true
            }

            // Tiempo máximo
            currentFilter.maxReadyTime?.let {
                sliderMaxTime.value = it.toFloat()
                switchMaxTime.isChecked = true
                tvMaxTimeValue.text = it.toString()
            } ?: run {
                switchMaxTime.isChecked = false
                sliderMaxTime.isEnabled = false
                sliderMaxTime.value = 60f
                tvMaxTimeValue.text = "60"
            }

            // Health Score mínimo
            currentFilter.minHealthScore?.let {
                sliderMinHealth.value = it.toFloat()
                switchMinHealth.isChecked = true
                tvMinHealthValue.text = it.toString()
            } ?: run {
                switchMinHealth.isChecked = false
                sliderMinHealth.isEnabled = false
                sliderMinHealth.value = 50f
                tvMinHealthValue.text = "50"
            }

            // Calorías máximas
            currentFilter.maxCalories?.let {
                sliderMaxCalories.value = it.toFloat()
                switchMaxCalories.isChecked = true
                tvMaxCaloriesValue.text = it.toString()
            } ?: run {
                switchMaxCalories.isChecked = false
                sliderMaxCalories.isEnabled = false
                sliderMaxCalories.value = 500f
                tvMaxCaloriesValue.text = "500"
            }

            // Dietas
            chipVegetarian.isChecked = currentFilter.vegetarian == true
            chipVegan.isChecked = currentFilter.vegan == true
            chipGlutenFree.isChecked = currentFilter.glutenFree == true
            chipDairyFree.isChecked = currentFilter.dairyFree == true

            // Ordenamiento
            when (currentFilter.sortBy) {
                SortOption.POPULARITY -> chipSortPopularity.isChecked = true
                SortOption.TIME -> chipSortTime.isChecked = true
                SortOption.HEALTH_SCORE -> chipSortHealth.isChecked = true
                SortOption.CALORIES -> chipSortCalories.isChecked = true
                SortOption.PRICE -> chipSortPrice.isChecked = true
            }
        }
    }

    private fun applyFilters() {
        binding.apply {
            val category = if (spinnerCategory.selectedItemPosition == 0) {
                null
            } else {
                RecipeCategories.ALL[spinnerCategory.selectedItemPosition - 1]
            }

            val area = if (spinnerArea.selectedItemPosition == 0) {
                null
            } else {
                RecipeAreas.ALL[spinnerArea.selectedItemPosition - 1]
            }

            val source = when {
                chipSourceMealdb.isChecked -> RecipeSource.THEMEALDB
                chipSourceSpoonacular.isChecked -> RecipeSource.SPOONACULAR
                else -> null
            }

            val maxReadyTime = if (switchMaxTime.isChecked) {
                sliderMaxTime.value.toInt()
            } else null

            val minHealthScore = if (switchMinHealth.isChecked) {
                sliderMinHealth.value.toInt()
            } else null

            val maxCalories = if (switchMaxCalories.isChecked) {
                sliderMaxCalories.value.toInt()
            } else null

            val vegetarian = if (chipVegetarian.isChecked) true else null
            val vegan = if (chipVegan.isChecked) true else null
            val glutenFree = if (chipGlutenFree.isChecked) true else null
            val dairyFree = if (chipDairyFree.isChecked) true else null

            val sortBy = when {
                chipSortTime.isChecked -> SortOption.TIME
                chipSortHealth.isChecked -> SortOption.HEALTH_SCORE
                chipSortCalories.isChecked -> SortOption.CALORIES
                chipSortPrice.isChecked -> SortOption.PRICE
                else -> SortOption.POPULARITY
            }

            val filter = currentFilter.copy(
                category = category,
                area = area,
                source = source,
                maxReadyTime = maxReadyTime,
                minHealthScore = minHealthScore,
                maxCalories = maxCalories,
                vegetarian = vegetarian,
                vegan = vegan,
                glutenFree = glutenFree,
                dairyFree = dairyFree,
                sortBy = sortBy
            )

            onFilterApplied?.invoke(filter)
            dismiss()
        }
    }

    private fun clearFilters() {
        val filter = RecipeFilter(query = currentFilter.query)
        onFilterApplied?.invoke(filter)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            currentFilter: RecipeFilter,
            onFilterApplied: (RecipeFilter) -> Unit
        ): FilterBottomSheetFragment {
            return FilterBottomSheetFragment().apply {
                this.currentFilter = currentFilter
                this.onFilterApplied = onFilterApplied
            }
        }
    }
}