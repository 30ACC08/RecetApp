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
import com.example.recetapp.data.model.User
import com.example.recetapp.databinding.FragmentFavoritosBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.example.recetapp.ui.viewmodel.UiState

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
        setupObservers()
        viewModel.loadFavorites()
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                viewModel.setSelectedRecipe(recipe)
                findNavController().navigate(R.id.action_favoritosFragment_to_detalleFragment)
            },
            onFavoriteClick = { recipe -> viewModel.toggleFavorite(recipe) },
            onUserClick = { userId ->
                val user = User(id = userId)
                val bundle = Bundle().apply { putParcelable("user", user) }
                findNavController().navigate(R.id.action_global_publicProfileFragment, bundle)
            }
        )
        binding.rvFavoritos.layoutManager = LinearLayoutManager(context)
        binding.rvFavoritos.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.favoritesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvFavoritos.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    adapter.submitList(state.data)
                    binding.tvCantidad.text = "${state.data.size} recetas guardadas"
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvFavoritos.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvCantidad.text = "0 recetas guardadas"
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}