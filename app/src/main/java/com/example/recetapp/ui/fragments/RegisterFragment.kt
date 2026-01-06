package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentRegisterBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // OBSERVADORES
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Cuenta creada", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Volver al login
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.validationError.observe(viewLifecycleOwner) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            // ID XML: btn_registrar
            binding.btnRegistrar.isEnabled = !loading
            binding.btnRegistrar.text = if (loading) "Creando..." else getString(R.string.crear_cuenta)
        }

        // LISTENERS
        // ID XML: btn_back
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // ID XML: btn_registrar
        binding.btnRegistrar.setOnClickListener {
            // IDs XML: et_nombre, et_email, et_password, et_confirmar_password
            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirm = binding.etConfirmarPassword.text.toString()

            viewModel.register(nombre, email, pass, confirm)
        }

        // ID XML: tv_iniciar_sesion
        binding.tvIniciarSesion.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}