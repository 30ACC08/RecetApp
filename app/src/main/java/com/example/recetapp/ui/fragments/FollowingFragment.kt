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
import com.example.recetapp.databinding.FragmentAdminBinding
import com.example.recetapp.ui.adapters.UsersAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel

class FollowingFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvHeaderTitle.text = "Mi Comunidad"
        binding.tvTitulo.text = "Personas que sigo"
        binding.cvStats.visibility = View.GONE
        binding.tvListaTitulo.visibility = View.GONE

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.loadFollowingList()

        viewModel.followingList.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUsers.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE

                val adapter = UsersAdapter(
                    users = users,
                    isAdmin = false,
                    onEditClick = {},
                    onDeleteClick = {},
                    onUserClick = { user ->
                        // NAVEGAR AL PERFIL PÚBLICO DEL USUARIO SELECCIONADO
                        val bundle = Bundle().apply { putParcelable("user", user) }
                        findNavController().navigate(R.id.action_followingFragment_to_publicProfileFragment, bundle)
                    }
                )
                binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
                binding.recyclerViewUsers.adapter = adapter
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}