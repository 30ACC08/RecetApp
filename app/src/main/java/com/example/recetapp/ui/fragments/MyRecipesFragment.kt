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
import com.example.recetapp.databinding.FragmentMyRecipesBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.example.recetapp.ui.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MyRecipesFragment : Fragment() {

    private var _binding: FragmentMyRecipesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        viewModel.loadMyRecipes()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_myRecipesFragment_to_createRecipeFragment)
        }

        adapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                val bundle = Bundle().apply { putParcelable("recipe", recipe) }
                findNavController().navigate(R.id.action_myRecipesFragment_to_createRecipeFragment, bundle)
            },
            onFavoriteClick = { recipe -> showDeleteDialog(recipe) },
            onUserClick = {}, // Sin acción en mis recetas
            isMyRecipesMode = true
        )

        binding.rvMyRecipes.layoutManager = LinearLayoutManager(context)
        binding.rvMyRecipes.adapter = adapter
    }

    // ... (setupObservers, showDeleteDialog, onDestroyView igual que original)
    private fun setupObservers() {
        viewModel.myRecipesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvMyRecipes.visibility = View.VISIBLE
                    binding.llEmpty.visibility = View.GONE
                    adapter.submitList(state.data)
                    binding.tvCantidad.text = "${state.data.size} recetas publicadas"
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvMyRecipes.visibility = View.GONE
                    binding.llEmpty.visibility = View.VISIBLE
                    binding.tvCantidad.text = "0 recetas publicadas"
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteDialog(recipe: Recipe) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Receta")
            .setMessage("¿Estás seguro de borrar '${recipe.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteRecipe(recipe.id)
                Toast.makeText(context, "Eliminando...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}