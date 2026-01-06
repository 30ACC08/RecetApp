package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentSplashBinding
import com.example.recetapp.ui.viewmodel.AuthViewModel

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Esperar 2 segundos y verificar sesión
        Handler(Looper.getMainLooper()).postDelayed({
            // CORRECCIÓN: Usamos currentUser.value en lugar de isLoggedIn()
            // AuthViewModel carga el usuario automáticamente al iniciarse
            if (viewModel.currentUser.value != null) {
                // Usuario logueado -> Ir a Inicio
                findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                // No logueado -> Ir a Login
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }, 2000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}