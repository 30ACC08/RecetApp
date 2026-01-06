package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.databinding.FragmentPerfilBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import com.example.recetapp.ui.viewmodel.UiState
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
        recipeViewModel.loadMyRecipes()
        recipeViewModel.loadUserReviews()
        authViewModel.loadUserStats()
    }

    private fun loadUserData() {
        lifecycleScope.launch { authViewModel.loadCurrentUser() }
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvNombre.text = user.nombre
                if (user.photoUrl.isNotEmpty()) {
                    Glide.with(this).load(user.photoUrl).circleCrop().into(binding.ivPerfil)
                } else {
                    binding.ivPerfil.setImageResource(R.drawable.ic_person)
                }

                if (user.rol == UserRole.ADMIN) {
                    binding.tvTipo.text = "Administrador"
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    binding.llAdmin.visibility = View.VISIBLE
                } else {
                    binding.tvTipo.text = "Chef Aficionado"
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    binding.llAdmin.visibility = View.GONE
                }
            }
        }
    }

    private fun setupObservers() {
        authViewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch { authViewModel.loadCurrentUser() }
            }
        }

        // Estadísticas de Seguidores / Siguiendo
        authViewModel.userStats.observe(viewLifecycleOwner) { (followers, following) ->
            binding.tvSeguidores.text = followers.toString()
            binding.tvSiguiendo.text = following.toString() // Ahora sí tenemos este TextView
        }

        recipeViewModel.favoritesState.observe(viewLifecycleOwner) { state ->
            val count = if (state is UiState.Success) state.data.size else 0
            binding.tvFavoritos.text = count.toString()
        }

        recipeViewModel.myRecipesState.observe(viewLifecycleOwner) { state ->
            val count = if (state is UiState.Success) state.data.size else 0
            binding.tvRecetas.text = count.toString()
        }
    }

    private fun setupClickListeners() {
        binding.ivPerfil.setOnClickListener { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

        // Clic en la estadística "Siguiendo" -> Abre lista de personas seguidas
        binding.llStatSiguiendo.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_followingFragment) }

        // Clic en la estadística "Seguidores" -> Por ahora muestra un mensaje (o podrías crear un fragmento de seguidores)
        binding.llStatSeguidores.setOnClickListener {
            // Podrías reutilizar el adapter de usuarios si tuvieras la lógica de backend para traer seguidores
            Toast.makeText(context, "Lista de seguidores próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.llMisRecetas.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_myRecipesFragment) }
        binding.llFavoritos.setOnClickListener { findNavController().navigate(R.id.favoritosFragment) }
        binding.llAdmin.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_adminFragment) }
        binding.llResenas.setOnClickListener { findNavController().navigate(R.id.userReviewsFragment) }

        binding.llCerrarSesion.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }

        // Nuevo menú de Configuración
        binding.llPreferencias.setOnClickListener { showSettingsDialog() }

        binding.llNotificaciones.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_notificationsFragment) }
        binding.llAyuda.setOnClickListener { Toast.makeText(context, "Soporte: help@recetapp.com", Toast.LENGTH_SHORT).show() }
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Editar Nombre", "Cambiar Foto", "Eliminar Cuenta", "Cancelar")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configuración de Cuenta")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditNameDialog()
                    1 -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    2 -> showDeleteAccountDialog()
                    3 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showEditNameDialog() {
        val editText = EditText(context)
        editText.hint = "Nuevo nombre"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actualizar Nombre")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    val user = authViewModel.currentUser.value
                    user?.let { authViewModel.updateUserProfile(it.id, newName, null) }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro? Se borrará todo permanentemente.")
            .setPositiveButton("Eliminar") { _, _ ->
                authViewModel.currentUser.value?.let {
                    authViewModel.deleteUser(it.id, it.email)
                    authViewModel.logout()
                    findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
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