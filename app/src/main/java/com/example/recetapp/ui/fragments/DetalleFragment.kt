package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recetapp.databinding.FragmentDetalleBinding

class DetalleFragment : Fragment() {

    private var _binding: FragmentDetalleBinding? = null
    private val binding get() = _binding!!
    private var esFavorito = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivFavorito.setOnClickListener {
            esFavorito = !esFavorito
            binding.ivFavorito.isSelected = esFavorito
            val mensaje = if (esFavorito) "Agregado a favoritos" else "Eliminado de favoritos"
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }

        binding.btnComenzarCocinar.setOnClickListener {
            Toast.makeText(context, "Iniciando modo cocina...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}