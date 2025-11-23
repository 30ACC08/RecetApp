package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.repository.RecipeRepository
import com.example.recetapp.databinding.FragmentMyRecipesBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MyRecipesFragment : Fragment() {

    private var _binding: FragmentMyRecipesBinding? = null
    private val binding get() = _binding!!
    private val repository = RecipeRepository()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadMyRecipes()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Botón Flotante para crear
        binding.btnAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_myRecipesFragment_to_createRecipeFragment)
        }

        // Adapter con flag TRUE para mostrar botón de borrar
        adapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                val bundle = Bundle().apply { putParcelable("recipe", recipe) }
                findNavController().navigate(R.id.action_myRecipesFragment_to_createRecipeFragment, bundle)
            },
            onFavoriteClick = { recipe ->
                showDeleteDialog(recipe)
            },
            isMyRecipesMode = true // <--- IMPORTANTE: Activa modo "Mis Recetas"
        )

        binding.rvMyRecipes.layoutManager = LinearLayoutManager(context)
        binding.rvMyRecipes.adapter = adapter
    }

    private fun loadMyRecipes() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getUserRecipes().onSuccess { recipes ->
                adapter.submitList(recipes)
                binding.tvCantidad.text = "${recipes.size} recetas publicadas"
                binding.llEmpty.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
                binding.rvMyRecipes.visibility = if (recipes.isEmpty()) View.GONE else View.VISIBLE
            }.onFailure {
                Toast.makeText(context, "Error cargando recetas", Toast.LENGTH_SHORT).show()
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showDeleteDialog(recipe: Recipe) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Receta")
            .setMessage("¿Estás seguro de borrar '${recipe.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteRecipe(recipe.id)
                    loadMyRecipes()
                    Toast.makeText(context, "Eliminada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}