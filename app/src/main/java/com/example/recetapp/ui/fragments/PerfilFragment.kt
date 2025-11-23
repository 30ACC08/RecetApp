package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        // 1. Cargar FAVORITOS
        recipeViewModel.loadFavorites()
        recipeViewModel.favorites.observe(viewLifecycleOwner) { favs ->
            binding.tvFavoritos.text = favs.size.toString()
            val textFavList = binding.llFavoritos.getChildAt(1) as? android.widget.TextView
            textFavList?.text = favs.size.toString()
        }

        // 2. Cargar MIS RECETAS (Corrección)
        recipeViewModel.loadMyRecipes()
        recipeViewModel.myRecipes.observe(viewLifecycleOwner) { recipes ->
            // Actualizar el contador grande
            binding.tvRecetas.text = recipes.size.toString()

            // Actualizar el contador pequeño en la lista
            val textMyRecipesList = binding.llMisRecetas.getChildAt(1) as? android.widget.TextView
            textMyRecipesList?.text = recipes.size.toString()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch { authViewModel.loadCurrentUser() }
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvNombre.text = user.nombre
                if (user.rol == UserRole.ADMIN) {
                    binding.tvTipo.text = getString(R.string.administrador)
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                    binding.llAdmin.visibility = View.VISIBLE
                } else {
                    binding.tvTipo.text = getString(R.string.chef_aficionada)
                    binding.tvTipo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.llAdmin.visibility = View.GONE
                }
            } else {
                findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
            }
        }
    }

    private fun setupClickListeners() {
        binding.llMisRecetas.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_myRecipesFragment) }
        binding.llFavoritos.setOnClickListener { findNavController().navigate(R.id.favoritosFragment) }
        binding.llAdmin.setOnClickListener { findNavController().navigate(R.id.action_perfilFragment_to_adminFragment) }

        binding.llCerrarSesion.setOnClickListener {
            authViewModel.logout()
            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }

        binding.llSiguiendo.setOnClickListener { Toast.makeText(context, "Próximamente: Siguiendo", Toast.LENGTH_SHORT).show() }
        binding.llResenas.setOnClickListener { Toast.makeText(context, "Próximamente: Reseñas", Toast.LENGTH_SHORT).show() }
        binding.llNotificaciones.setOnClickListener { Toast.makeText(context, "Sin notificaciones", Toast.LENGTH_SHORT).show() }
        binding.llPreferencias.setOnClickListener { Toast.makeText(context, "Configuración", Toast.LENGTH_SHORT).show() }
        binding.llAyuda.setOnClickListener { Toast.makeText(context, "Ayuda", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}