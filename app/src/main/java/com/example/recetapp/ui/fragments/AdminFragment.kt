package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.R
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.databinding.FragmentAdminBinding
import com.example.recetapp.ui.adapters.UsersAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch { viewModel.loadCurrentUser() }
        setupClickListeners()
        setupRecyclerView()
        setupObservers()

        // Cargar usuarios al iniciar
        viewModel.loadAllUsers()
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(
            isAdminMode = true, // Activamos modo admin
            onEditClick = { user -> showEditDialog(user) },
            onDeleteClick = { user -> showDeleteConfirmation(user) }
        )
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewUsers.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupObservers() {
        // Verificar Rol
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null && user.rol != UserRole.ADMIN) {
                Toast.makeText(context, "Acceso denegado", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

        // Lista de Usuarios
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            binding.tvTotalUsuarios.text = users.size.toString()

            // Usamos submitList del ListAdapter
            adapter.submitList(users)

            if (users.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUsers.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE
            }
        }

        viewModel.generalError.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show() }
            result.onFailure { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showEditDialog(user: User) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_user, null)
        val etNombre = view.findViewById<EditText>(R.id.et_nombre)
        etNombre.setText(user.nombre)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Usuario")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                if (nuevoNombre.isNotBlank()) {
                    viewModel.updateUser(user.id, nuevoNombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(user: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de eliminar a ${user.nombre}?\nEsta acción es irreversible.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteUser(user.id, user.email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}