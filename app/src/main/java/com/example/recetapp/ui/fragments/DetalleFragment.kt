package com.example.recetapp.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.RecipeSource
import com.example.recetapp.data.model.RecipeTranslations
import com.example.recetapp.databinding.FragmentDetalleBinding
import com.example.recetapp.ui.adapters.ReviewAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class DetalleFragment : Fragment() {

    private var _binding: FragmentDetalleBinding? = null
    private val binding get() = _binding!!

    // ViewModel compartido para datos de la receta
    private val viewModel: RecipeViewModel by activityViewModels()

    // ViewModel propio del fragmento para acciones de usuario (seguir)
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var reviewsAdapter: ReviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupReviewsRecycler()
        setupObservers()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.fabFavorite.setOnClickListener {
            val recipe = viewModel.selectedRecipe.value
            if (recipe != null) {
                binding.fabFavorite.isEnabled = false
                viewModel.toggleFavorite(recipe)
            }
        }

        binding.btnTranslate.setOnClickListener { viewModel.toggleTranslation() }

        // Botón Seguir: verifica que authViewModel tenga la función toggleFollow
        binding.btnFollow.setOnClickListener {
            val recipe = viewModel.selectedRecipe.value
            if (recipe != null && recipe.userId.isNotBlank()) {
                authViewModel.toggleFollow(recipe.userId)
            }
        }

        binding.btnVideo.setOnClickListener {
            val url = viewModel.selectedRecipe.value?.videoUrl
            if (!url.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al abrir video", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No hay video", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddReview.setOnClickListener { showAddReviewDialog() }
    }

    private fun setupReviewsRecycler() {
        reviewsAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reviewsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Datos de receta
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                bindRecipeData(recipe)
                viewModel.loadReviews(recipe.id)
                viewModel.checkIfFavorite(recipe.id)
            }
        }

        // Estado "Seguiendo" (debe venir de AuthViewModel actualizado)
        authViewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Siguiendo"
                binding.btnFollow.setBackgroundColor(Color.GRAY)
            } else {
                binding.btnFollow.text = "Seguir"
                binding.btnFollow.setBackgroundColor(requireContext().getColor(R.color.orange_primary))
            }
        }

        viewModel.isRecipeFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.fabFavorite.isEnabled = true
            val icon = if (isFav == true) R.drawable.ic_favorite else android.R.drawable.star_big_off
            binding.fabFavorite.setImageResource(icon)
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }

        viewModel.isTranslated.observe(viewLifecycleOwner) { translated ->
            binding.btnTranslate.text = if (translated) "Ver Original" else "Traducir"
        }

        viewModel.isLoadingAction.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnTranslate.isEnabled = !loading
        }

        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            reviewsAdapter.submitList(reviews)
            binding.tvNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
            binding.tvReviewsTitle.text = "Reseñas (${reviews.size})"
        }

        viewModel.reviewUploadResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                Toast.makeText(requireContext(), "¡Reseña publicada!", Toast.LENGTH_SHORT).show()
                // Recargar reseñas
                viewModel.selectedRecipe.value?.id?.let { id -> viewModel.loadReviews(id) }
            }
        }
    }

    private fun bindRecipeData(recipe: Recipe) {
        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.ivRecipe)

        binding.collapsingToolbar.title = recipe.name
        binding.tvName.text = recipe.name
        binding.tvCategory.text = "${RecipeTranslations.categoryName(recipe.category)} • ${RecipeTranslations.areaName(recipe.area)}"
        binding.tvTime.text = "${recipe.readyInMinutes ?: "?"} min"

        // Lógica de visualización del creador
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (recipe.source == RecipeSource.USER && recipe.userId.isNotBlank() && recipe.userId != currentUserId) {
            binding.llCreatorSection.visibility = View.VISIBLE
            binding.tvCreatorName.text = if (recipe.creatorName.isNotBlank()) recipe.creatorName else "Usuario"

            // Verificar si ya lo sigo (Esto requiere AuthViewModel actualizado)
            authViewModel.checkIfFollowing(recipe.userId)
        } else {
            binding.llCreatorSection.visibility = View.GONE
        }

        val ingText = StringBuilder()
        recipe.ingredients.forEach {
            if (it.name.isNotBlank()) {
                ingText.append("• ${it.name} (${it.measure})\n")
            }
        }
        binding.tvIngredients.text = ingText.toString().ifBlank { "Sin ingredientes" }

        val cleanInstructions = recipe.instructions.ifBlank { "Sin instrucciones" }
            .replace("\r\n", "<br>").replace("\n", "<br>")

        binding.tvInstructions.text = Html.fromHtml(cleanInstructions, Html.FROM_HTML_MODE_COMPACT)
        binding.btnVideo.visibility = if (recipe.videoUrl.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun showAddReviewDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_review, null)
        val ratingBar = view.findViewById<android.widget.RatingBar>(R.id.rating_bar)
        val etComment = view.findViewById<android.widget.EditText>(R.id.et_comment)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Calificar")
            .setView(view)
            .setPositiveButton("Enviar") { _, _ ->
                val id = viewModel.selectedRecipe.value?.id ?: return@setPositiveButton
                viewModel.submitReview(id, ratingBar.rating, etComment.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}