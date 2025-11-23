package com.example.recetapp.ui.fragments

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
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentPerfilBinding
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.example.recetapp.ui.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val recipeViewModel: RecipeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        setupClickListeners()

        // Cargar favoritos para mostrar el número
        recipeViewModel.loadFavorites()
        recipeViewModel.favorites.observe(viewLifecycleOwner) { favs ->
            binding.tvFavoritos.text = favs.size.toString()
            // Actualizar también el texto de la lista inferior si aplica
            val textFav = binding.llFavoritos.getChildAt(1) as? android.widget.TextView
            textFav?.text = favs.size.toString()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch { authViewModel.loadCurrentUser() }
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvNombre.text = user.nombre
                if (user.rol == UserRole.ADMIN) {
                    binding.tvTipo.text = "Administrador del Sistema"
                    binding.tvTipo.setTextColor(resources.getColor(R.color.error, null))
                    binding.llAdmin.visibility = View.VISIBLE
                } else {
                    binding.tvTipo.text = getString(R.string.chef_aficionada)
                    binding.llAdmin.visibility = View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.llFavoritos.setOnClickListener { findNavController().navigate(R.id.favoritosFragment) }
        binding.llAdmin.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_adminFragment) }
        binding.llCerrarSesion.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }
        // Otros botones
        binding.llMisRecetas.setOnClickListener { Toast.makeText(context, "Mis Recetas", Toast.LENGTH_SHORT).show() }
        binding.llSiguiendo.setOnClickListener { Toast.makeText(context, "Siguiendo", Toast.LENGTH_SHORT).show() }
        binding.llResenas.setOnClickListener { Toast.makeText(context, "Reseñas", Toast.LENGTH_SHORT).show() }
        binding.llNotificaciones.setOnClickListener { Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show() }
        binding.llPreferencias.setOnClickListener { Toast.makeText(context, "Preferencias", Toast.LENGTH_SHORT).show() }
        binding.llAyuda.setOnClickListener { Toast.makeText(context, "Ayuda", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}