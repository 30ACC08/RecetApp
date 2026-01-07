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

        // --- DETECTAR SI ES SEGUIDORES O SIGUIENDO ---
        val listType = arguments?.getString("listType") ?: "FOLLOWING"

        if (listType == "FOLLOWERS") {
            binding.tvTitle.text = "Seguidores"
            binding.tvEmpty.text = "Aún no tienes seguidores"
        } else {
            binding.tvTitle.text = "Siguiendo"
            binding.tvEmpty.text = "Aún no sigues a nadie"
        }

        adapter = UsersAdapter(
            isAdminMode = false,
            onUserClick = { user ->
                val bundle = Bundle().apply { putParcelable("user", user) }
                findNavController().navigate(R.id.action_global_publicProfileFragment, bundle)
            }
        )

        binding.rvFollowing.layoutManager = LinearLayoutManager(context)
        binding.rvFollowing.adapter = adapter

        // --- CARGAR DATOS SEGÚN TIPO ---
        if (listType == "FOLLOWERS") {
            viewModel.followersList.observe(viewLifecycleOwner) { users ->
                adapter.submitList(users)
                binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
            viewModel.loadFollowersList()
        } else {
            viewModel.followingList.observe(viewLifecycleOwner) { users ->
                adapter.submitList(users)
                binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
            viewModel.loadFollowingList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}