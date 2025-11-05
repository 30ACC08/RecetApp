package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llMisRecetas.setOnClickListener {
            Toast.makeText(context, "Mis Recetas", Toast.LENGTH_SHORT).show()
        }

        binding.llFavoritos.setOnClickListener {
            findNavController().navigate(R.id.favoritosFragment)
        }

        binding.llSiguiendo.setOnClickListener {
            Toast.makeText(context, "Siguiendo", Toast.LENGTH_SHORT).show()
        }

        binding.llResenas.setOnClickListener {
            Toast.makeText(context, "Reseñas", Toast.LENGTH_SHORT).show()
        }

        binding.llNotificaciones.setOnClickListener {
            Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.llPreferencias.setOnClickListener {
            Toast.makeText(context, "Preferencias", Toast.LENGTH_SHORT).show()
        }

        binding.llAyuda.setOnClickListener {
            Toast.makeText(context, "Ayuda", Toast.LENGTH_SHORT).show()
        }

        binding.llCerrarSesion.setOnClickListener {
            Toast.makeText(context, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_perfilFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}