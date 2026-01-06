package com.example.recetapp.ui.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import com.example.recetapp.data.model.User
import com.example.recetapp.databinding.FragmentPublicProfileBinding
import com.example.recetapp.ui.adapters.RecipeAdapter
import com.example.recetapp.ui.adapters.UserReviewsAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

class PublicProfileFragment : Fragment() {

    private var _binding: FragmentPublicProfileBinding? = null
    private val binding get() = _binding!!
    private val recipeViewModel: RecipeViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var user: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPublicProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("user", User::class.java)
        } else {
            arguments?.getParcelable("user")
        }
        if (user == null) {
            Toast.makeText(context, "Error cargando perfil", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupUI()
        checkFollowStatus()
        loadContent()
        setupObservers()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvUserName.text = user?.nombre
        if (!user?.photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(user?.photoUrl).circleCrop().into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_person)
        }

        binding.btnFollow.setOnClickListener {
            user?.id?.let { targetId -> authViewModel.toggleFollow(targetId) }
        }

        val recipeAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                recipeViewModel.setSelectedRecipe(recipe)
                findNavController().navigate(R.id.action_publicProfileFragment_to_detalleFragment)
            },
            onFavoriteClick = { recipe ->
                recipeViewModel.toggleFavorite(recipe)
                Toast.makeText(context, "Actualizando favoritos", Toast.LENGTH_SHORT).show()
            },
            onUserClick = {
                // Ya estamos en perfil, no hacemos nada
            },
            isMyRecipesMode = false
        )
        binding.rvRecipes.layoutManager = LinearLayoutManager(context)
        binding.rvRecipes.adapter = recipeAdapter

        val reviewsAdapter = UserReviewsAdapter(
            onRecipeClick = { review ->
                val tempRecipe = Recipe(id = review.recipeId, name = review.recipeName, imageUrl = review.recipeImageUrl)
                recipeViewModel.setSelectedRecipe(tempRecipe)
                recipeViewModel.loadFullRecipeDetails(review.recipeId)
                findNavController().navigate(R.id.action_publicProfileFragment_to_detalleFragment)
            },
            isEditable = false
        )
        binding.rvReviews.layoutManager = LinearLayoutManager(context)
        binding.rvReviews.adapter = reviewsAdapter
    }

    private fun checkFollowStatus() {
        lifecycleScope.launch { authViewModel.loadCurrentUser() }
        user?.id?.let { authViewModel.checkIfFollowing(it) }
    }

    private fun loadContent() {
        user?.id?.let { userId -> recipeViewModel.loadPublicUserContent(userId) }
    }

    private fun setupObservers() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
            if (currentUser != null && user != null) {
                if (currentUser.id == user!!.id) {
                    binding.btnFollow.visibility = View.GONE
                } else {
                    binding.btnFollow.visibility = View.VISIBLE
                }
            }
        }
        authViewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Siguiendo"
                binding.btnFollow.setBackgroundColor(Color.GRAY)
            } else {
                binding.btnFollow.text = "Seguir"
                binding.btnFollow.setBackgroundColor(requireContext().getColor(R.color.orange_primary))
            }
        }
        recipeViewModel.publicUserRecipes.observe(viewLifecycleOwner) { recipes ->
            (binding.rvRecipes.adapter as RecipeAdapter).submitList(recipes)
            binding.tvNoRecipes.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }
        recipeViewModel.publicUserReviews.observe(viewLifecycleOwner) { reviews ->
            (binding.rvReviews.adapter as UserReviewsAdapter).submitList(reviews)
            binding.tvNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
        }
        recipeViewModel.isLoadingAction.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}