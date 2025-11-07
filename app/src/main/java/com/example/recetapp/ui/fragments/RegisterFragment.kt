package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recetapp.databinding.FragmentRegisterBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure { error ->
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.validationError.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRegistrar.setOnClickListener {
            if (!binding.cbTerminos.isChecked) {
                Toast.makeText(
                    context,
                    "Debes aceptar los términos y condiciones",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmarPassword = binding.etConfirmarPassword.text.toString()

            viewModel.register(nombre, email, password, confirmarPassword)
        }

        binding.tvIniciarSesion.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}