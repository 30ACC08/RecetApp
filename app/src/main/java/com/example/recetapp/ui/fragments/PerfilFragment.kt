package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentPerfilBinding
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val recipeViewModel: RecipeViewModel by activityViewModels()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val user = authViewModel.currentUser.value
            if (user != null) {
                Toast.makeText(context, "Subiendo foto...", Toast.LENGTH_SHORT).show()
                authViewModel.updateUserProfile(user.id, user.nombre, uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupClickListeners()
        setupObservers()

        recipeViewModel.loadFavorites()
        recipeViewModel.favorites.observe(viewLifecycleOwner) { favs ->
            binding.tvFavoritos.text = favs.size.toString()
            val textFavList = binding.llFavoritos.getChildAt(1) as? TextView
            textFavList?.text = favs.size.toString()
        }

        recipeViewModel.loadMyRecipes()
        recipeViewModel.myRecipes.observe(viewLifecycleOwner) { recipes ->
            binding.tvRecetas.text = recipes.size.toString()
            val textMyRecipesList = binding.llMisRecetas.getChildAt(1) as? TextView
            textMyRecipesList?.text = recipes.size.toString()
        }

        recipeViewModel.loadUserReviews()
        recipeViewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            val textReviewsList = binding.llResenas.getChildAt(1) as? TextView
            textReviewsList?.text = reviews.size.toString()
        }

        authViewModel.loadUserStats()
        authViewModel.userStats.observe(viewLifecycleOwner) { (followers, following) ->
            binding.tvSeguidores.text = followers.toString()
            val textFollowingList = binding.llSiguiendo.getChildAt(1) as? TextView
            textFollowingList?.text = following.toString()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch { authViewModel.loadCurrentUser() }
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvNombre.text = user.nombre
                if (user.photoUrl.isNotEmpty()) {
                    Glide.with(this).load(user.photoUrl).circleCrop().into(binding.ivPerfil)
                }
                if (user.rol == UserRole.ADMIN) {
                    binding.tvTipo.text = getString(R.string.administrador)
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                    binding.llAdmin.visibility = View.VISIBLE
                } else {
                    binding.tvTipo.text = getString(R.string.chef_aficionada)
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.llAdmin.visibility = View.GONE
                }
            }
        }
    }

    private fun setupObservers() {
        authViewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "¡Foto actualizada!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch { authViewModel.loadCurrentUser() }
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivPerfil.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.llMisRecetas.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_myRecipesFragment) }
        binding.llFavoritos.setOnClickListener { findNavController().navigate(R.id.favoritosFragment) }
        binding.llAdmin.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_adminFragment) }
        binding.llResenas.setOnClickListener { findNavController().navigate(R.id.userReviewsFragment) }
        binding.llCerrarSesion.setOnClickListener {
            authViewModel.logout()
            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }
        binding.llPreferencias.setOnClickListener { showDeleteAccountDialog() }
        binding.llSiguiendo.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_followingFragment) }

        // NUEVO: Navegar a Notificaciones
        binding.llNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_notificationsFragment)
        }

        binding.llAyuda.setOnClickListener { Toast.makeText(context, "Ayuda: contact@recetapp.com", Toast.LENGTH_SHORT).show() }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configuración de Cuenta")
            .setMessage("¿Deseas eliminar tu cuenta permanentemente? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("ELIMINAR") { _, _ ->
                val user = authViewModel.currentUser.value
                if (user != null) {
                    authViewModel.deleteUser(user.id, user.email)
                    authViewModel.logout()
                    findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
                    Toast.makeText(context, "Cuenta eliminada", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}