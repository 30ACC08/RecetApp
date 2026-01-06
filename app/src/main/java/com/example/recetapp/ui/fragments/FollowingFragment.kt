package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.databinding.FragmentFollowingBinding
import com.example.recetapp.ui.adapters.UsersAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel

class FollowingFragment : Fragment() {

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón atrás
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // --- AQUÍ ESTÁ EL CAMBIO PRINCIPAL ---
        // Instanciamos el adaptador indicando que NO es modo admin
        adapter = UsersAdapter(
            isAdminMode = false,
            onUserClick = { user ->
                // Al hacer clic, navegamos al perfil público de ese usuario
                val bundle = Bundle().apply { putParcelable("user", user) }
                findNavController().navigate(R.id.action_global_publicProfileFragment, bundle)
            }
        )

        binding.rvFollowing.layoutManager = LinearLayoutManager(context)
        binding.rvFollowing.adapter = adapter

        // Observar la lista de seguidos
        viewModel.followingList.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            // Mostrar mensaje si la lista está vacía (asegúrate de tener tvEmpty en tu XML)
            binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }

        // Cargar datos
        viewModel.loadFollowingList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}