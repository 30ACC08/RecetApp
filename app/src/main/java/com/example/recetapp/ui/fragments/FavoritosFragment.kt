package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentFavoritosBinding

class FavoritosFragment : Fragment() {

    private var _binding: FragmentFavoritosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Click en recetas favoritas
        binding.cvFavorito1.setOnClickListener {
            findNavController().navigate(R.id.action_favoritosFragment_to_detalleFragment)
        }

        binding.cvFavorito2.setOnClickListener {
            findNavController().navigate(R.id.action_favoritosFragment_to_detalleFragment)
        }

        binding.cvFavorito3.setOnClickListener {
            findNavController().navigate(R.id.action_favoritosFragment_to_detalleFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}