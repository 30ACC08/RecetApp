package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.recetapp.data.model.*
import com.example.recetapp.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var currentFilter: RecipeFilter = RecipeFilter()
    private var onFilterApplied: ((RecipeFilter) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupSliders()
        setupListeners()
        loadCurrentFilter()
    }

    private fun setupSpinners() {
        // Categorías en español
        val categories = listOf("Todas") + RecipeTranslations.CATEGORIES.values.toList()
        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = catAdapter

        // Áreas en español
        val areas = listOf("Todas") + RecipeTranslations.AREAS.values.toList()
        val areaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, areas)
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerArea.adapter = areaAdapter
    }

    private fun setupSliders() {
        binding.sliderMaxTime.addOnChangeListener { _, v, _ -> binding.tvMaxTimeValue.text = v.toInt().toString() }
        binding.switchMaxTime.setOnCheckedChangeListener { _, isChecked -> binding.sliderMaxTime.isEnabled = isChecked }
    }

    private fun setupListeners() {
        binding.btnApplyFilters.setOnClickListener { applyFilters() }
        binding.btnClearFilters.setOnClickListener {
            onFilterApplied?.invoke(RecipeFilter())
            dismiss()
        }
    }

    private fun loadCurrentFilter() {
        // Cargar valores previos (Traduciendo de vuelta)
        val catName = currentFilter.category?.let { RecipeTranslations.categoryName(it) }
        val catIndex = if (catName != null) RecipeTranslations.CATEGORIES.values.indexOf(catName) + 1 else 0
        binding.spinnerCategory.setSelection(if (catIndex > 0) catIndex else 0)

        currentFilter.maxReadyTime?.let {
            binding.sliderMaxTime.value = it.toFloat()
            binding.switchMaxTime.isChecked = true
        }
    }

    private fun applyFilters() {
        // Traducir de Español a Inglés para la API
        val selectedCatEsp = binding.spinnerCategory.selectedItem as String
        val category = if (binding.spinnerCategory.selectedItemPosition == 0) null
        else RecipeTranslations.getCategoryKey(selectedCatEsp)

        val maxReadyTime = if (binding.switchMaxTime.isChecked) binding.sliderMaxTime.value.toInt() else null

        val filter = currentFilter.copy(category = category, maxReadyTime = maxReadyTime)
        onFilterApplied?.invoke(filter)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(filter: RecipeFilter, callback: (RecipeFilter) -> Unit) =
            FilterBottomSheetFragment().apply { currentFilter = filter; onFilterApplied = callback }
    }
}