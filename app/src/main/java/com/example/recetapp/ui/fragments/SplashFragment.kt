package com.example.recetapp.ui.fragments

import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth

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

        // Usamos un pequeño delay estético, pero la lógica no depende de él para saber si estás logueado
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 1500)
    }

    private fun checkSession() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Verificar la preferencia "Recordarme"
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("RECORDARME_PREF", false)

        if (currentUser != null) {
            // Caso: Hay usuario en Firebase
            if (rememberMe) {
                // Caso: El usuario marcó "Recordarme" -> Ir a Home
                // Pero antes, asegurarnos que los datos del usuario (Rol, Nombre) estén cargados en el ViewModel
                // para evitar el crash en la siguiente pantalla.

                // Observamos una vez para navegar
                viewModel.currentUser.observe(viewLifecycleOwner) { user ->
                    if (user != null) {
                        // Datos listos, navegar a Home
                        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                        // Remover observer para evitar múltiples navegaciones (técnica simple)
                        viewModel.currentUser.removeObservers(viewLifecycleOwner)
                    }
                }

                // Forzar carga si aún es nulo (seguridad)
                if (viewModel.currentUser.value == null) {
                    viewModel.loadCurrentUser()
                }

            } else {
                // Caso: Hay usuario pero NO marcó "Recordarme" -> Cerrar sesión y pedir login
                viewModel.logout()
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        } else {
            // Caso: No hay usuario en Firebase -> Ir a Login
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}