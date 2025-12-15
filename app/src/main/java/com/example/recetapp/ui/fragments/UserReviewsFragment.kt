package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.Review
import com.example.recetapp.databinding.FragmentUserReviewsBinding
import com.example.recetapp.ui.adapters.UserReviewsAdapter
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserReviewsFragment : Fragment() {

    private var _binding: FragmentUserReviewsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()
    private lateinit var adapter: UserReviewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        viewModel.loadUserReviews()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = UserReviewsAdapter(
            onRecipeClick = { review -> navigateToRecipeDetail(review) },
            onEditClick = { review -> showEditDialog(review) },
            onDeleteClick = { review -> showDeleteDialog(review) },
            isEditable = true
        )

        binding.rvUserReviews.layoutManager = LinearLayoutManager(context)
        binding.rvUserReviews.adapter = adapter
    }

    private fun navigateToRecipeDetail(review: Review) {
        val tempRecipe = Recipe(
            id = review.recipeId,
            name = review.recipeName,
            imageUrl = review.recipeImageUrl,
            thumbnailUrl = review.recipeImageUrl
        )
        viewModel.setSelectedRecipe(tempRecipe)
        viewModel.loadFullRecipeDetails(review.recipeId)

        try {
            findNavController().navigate(R.id.action_userReviewsFragment_to_detalleFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error de navegación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            adapter.submitList(reviews)
            binding.tvEmpty.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.reviewActionState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
            result.onFailure { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // CORRECCIÓN: Usamos isLoadingAction (la barra de carga general para acciones)
        // Nota: Si este ViewModel no expone isLoadingAction, asegúrate de haber actualizado RecipeViewModel.kt
        viewModel.isLoadingAction.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteDialog(review: Review) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Reseña")
            .setMessage("¿Borrar tu opinión sobre ${review.recipeName}?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteReview(review) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(review: Review) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_review, null)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar)
        val etComment = view.findViewById<EditText>(R.id.et_comment)

        ratingBar.rating = review.rating
        etComment.setText(review.comment)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Reseña")
            .setView(view)
            .setPositiveButton("Actualizar") { _, _ ->
                val newComment = etComment.text.toString().trim()
                if (newComment.isNotBlank()) {
                    viewModel.editReview(review, ratingBar.rating, newComment)
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