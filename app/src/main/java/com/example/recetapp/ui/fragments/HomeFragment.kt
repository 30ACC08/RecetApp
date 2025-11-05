package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Click en barra de búsqueda
        binding.cvBuscar.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        // Click en receta destacada
        binding.cvReceta1.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_detalleFragment)
        }

        binding.cvReceta2.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_detalleFragment)
        }

        binding.cvReceta3.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_detalleFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}