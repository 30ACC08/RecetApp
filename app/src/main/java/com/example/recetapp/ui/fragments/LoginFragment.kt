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
import com.example.recetapp.databinding.FragmentLoginBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                Toast.makeText(context, "Bienvenido ${user.nombre}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }.onFailure { error ->
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.validationError.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val rememberMe = binding.cbRecordarme.isChecked

            viewModel.login(email, password, rememberMe)
        }

        binding.tvRegistrarse.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}