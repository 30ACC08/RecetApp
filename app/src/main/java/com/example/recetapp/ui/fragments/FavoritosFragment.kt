package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentFavoritosBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel

class FavoritosFragment : Fragment() {

    private var _binding: FragmentFavoritosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.loadFavorites() // Cargar al entrar
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                viewModel.setSelectedRecipe(recipe)
                findNavController().navigate(R.id.action_favoritosFragment_to_detalleFragment)
            },
            onFavoriteClick = { recipe ->
                viewModel.toggleFavorite(recipe) // Permitir quitar desde aquí
            }
        )
        binding.rvFavoritos.layoutManager = LinearLayoutManager(context)
        binding.rvFavoritos.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.favorites.observe(viewLifecycleOwner) { recipes ->
            adapter.submitList(recipes)
            binding.tvCantidad.text = "${recipes.size} recetas guardadas"

            if (recipes.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvFavoritos.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvFavoritos.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}