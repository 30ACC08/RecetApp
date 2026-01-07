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

    private val viewModel: RecipeViewModel by activityViewModels()
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

        binding.btnFollow.setOnClickListener {
            val recipe = viewModel.selectedRecipe.value
            // Validación extra para evitar NullPointer si userId es nulo
            if (recipe != null && !recipe.userId.isNullOrBlank()) {
                authViewModel.toggleFollow(recipe.userId)
            }
        }

        binding.btnVideo.setOnClickListener {
            val url = viewModel.selectedRecipe.value?.videoUrl
            if (!url.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al abrir video o no hay app compatible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No hay video disponible", Toast.LENGTH_SHORT).show()
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
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                try {
                    bindRecipeData(recipe)
                    viewModel.loadReviews(recipe.id)
                    viewModel.checkIfFavorite(recipe.id)
                } catch (e: Exception) {
                    // Si falla el pintado, evitamos cierre forzoso
                    e.printStackTrace()
                    Toast.makeText(context, "Error visualizando receta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        authViewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Siguiendo"
                binding.btnFollow.setBackgroundColor(Color.GRAY)
            } else {
                binding.btnFollow.text = "Seguir"
                // Verificar que el contexto siga vivo
                context?.let {
                    binding.btnFollow.setBackgroundColor(it.getColor(R.color.orange_primary))
                }
            }
        }

        viewModel.isRecipeFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.fabFavorite.isEnabled = true
            val icon = if (isFav == true) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            // Usamos ic_favorite_border en lugar de android.R.drawable.star_big_off si lo tienes, o el que tengas
            try { binding.fabFavorite.setImageResource(icon) } catch (e: Exception) { /* ignore */ }
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
                viewModel.selectedRecipe.value?.id?.let { id -> viewModel.loadReviews(id) }
            }
        }
    }

    private fun bindRecipeData(recipe: Recipe) {
        // Uso seguro de Glide
        try {
            Glide.with(this)
                .load(recipe.imageUrl.takeIf { !it.isNullOrBlank() })
                .placeholder(R.drawable.ic_launcher_background) // Asegúrate de tener un placeholder válido
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.ivRecipe)
        } catch (e: Exception) { /* Ignorar error de carga de imagen */ }

        binding.collapsingToolbar.title = recipe.name ?: "Sin Nombre"
        binding.tvName.text = recipe.name ?: "Sin Nombre"

        // Protección contra nulls en RecipeTranslations
        val catName = RecipeTranslations.categoryName(recipe.category ?: "")
        val areaName = RecipeTranslations.areaName(recipe.area ?: "")
        binding.tvCategory.text = "$catName • $areaName"

        binding.tvTime.text = "${recipe.readyInMinutes ?: "?"} min"

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Verificar source y userId con seguridad
        val isUserRecipe = recipe.source == RecipeSource.USER
        val hasCreator = !recipe.userId.isNullOrBlank()
        val isNotMe = recipe.userId != currentUserId

        if (isUserRecipe && hasCreator && isNotMe) {
            binding.llCreatorSection.visibility = View.VISIBLE
            binding.tvCreatorName.text = if (!recipe.creatorName.isNullOrBlank()) recipe.creatorName else "Usuario"
            authViewModel.checkIfFollowing(recipe.userId)
        } else {
            binding.llCreatorSection.visibility = View.GONE
        }

        val ingText = StringBuilder()
        recipe.ingredients?.forEach {
            if (!it.name.isNullOrBlank()) {
                ingText.append("• ${it.name} (${it.measure ?: ""})\n")
            }
        }
        binding.tvIngredients.text = ingText.toString().ifBlank { "Sin ingredientes" }

        val instructions = recipe.instructions ?: ""
        val cleanInstructions = instructions.ifBlank { "Sin instrucciones detalladas." }
            .replace("\r\n", "<br>").replace("\n", "<br>")

        try {
            binding.tvInstructions.text = Html.fromHtml(cleanInstructions, Html.FROM_HTML_MODE_COMPACT)
        } catch (e: Exception) {
            binding.tvInstructions.text = cleanInstructions // Fallback a texto plano si falla HTML
        }

        binding.btnVideo.visibility = if (recipe.videoUrl.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun showAddReviewDialog() {
        try {
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
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir diálogo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}