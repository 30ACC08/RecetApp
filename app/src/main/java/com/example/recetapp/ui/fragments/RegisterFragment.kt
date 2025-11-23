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
        // Observar resultado de registro
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                android.util.Log.d("RegisterFragment", "✅ Registro exitoso: ${user.nombre}")
                Toast.makeText(
                    context,
                    "Cuenta creada exitosamente. Ahora puedes iniciar sesión",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }.onFailure { error ->
                android.util.Log.e("RegisterFragment", "❌ Error de registro: ${error.message}")
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        }

        // Observar errores de validación
        viewModel.validationError.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnRegistrar.isEnabled = !isLoading
            if (isLoading) {
                binding.btnRegistrar.text = "Creando cuenta..."
            } else {
                binding.btnRegistrar.text = "Crear Cuenta"
            }
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

            android.util.Log.d("RegisterFragment", "Intentando registrar usuario:")
            android.util.Log.d("RegisterFragment", "Nombre: $nombre")
            android.util.Log.d("RegisterFragment", "Email: $email")

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