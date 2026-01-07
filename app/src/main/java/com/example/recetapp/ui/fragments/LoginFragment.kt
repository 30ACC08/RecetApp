package com.example.recetapp.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentLoginBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperar si había marcado "Recordarme" anteriormente para dejar la casilla marcada visualmente
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val rememberMePrev = sharedPref.getBoolean("RECORDARME_PREF", false)
        binding.cbRecordarme.isChecked = rememberMePrev

        // OBSERVADORES
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Bienvenido", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.validationError.observe(viewLifecycleOwner) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.resetPasswordResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { Toast.makeText(context, "Correo enviado", Toast.LENGTH_SHORT).show() }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnLogin.isEnabled = !loading
            binding.btnLogin.text = if (loading) "Cargando..." else getString(R.string.iniciar_sesion)
        }

        // LISTENERS
        // ID XML: btn_login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val remember = binding.cbRecordarme.isChecked

            // GUARDAR PREFERENCIA "RECORDARME"
            // Esto es crucial para que el SplashFragment sepa qué hacer la próxima vez
            val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putBoolean("RECORDARME_PREF", remember)
                apply()
            }

            viewModel.login(email, pass, remember)
        }

        // ID XML: tv_olvide_password
        binding.tvOlvidePassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.tilEmail.error = "Ingresa tu correo"
            } else {
                viewModel.resetPassword(email)
            }
        }

        // ID XML: tv_registrarse
        binding.tvRegistrarse.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}