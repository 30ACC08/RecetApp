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
import com.example.recetapp.databinding.FragmentAdminBinding
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.ui.adapters.UsersAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.loadCurrentUser()
        }

        setupClickListeners()
        setupObservers()
        viewModel.loadAllUsers()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user?.rol != UserRole.ADMIN) {
                Toast.makeText(context, "No tienes permisos de administrador", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            binding.tvTotalUsuarios.text = users.size.toString()

            if (users.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUsers.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE

                val adapter = UsersAdapter(
                    users = users,
                    isAdmin = true,
                    onEditClick = { user -> showEditDialog(user) },
                    onDeleteClick = { user -> showDeleteConfirmation(user) }
                )

                binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
                binding.recyclerViewUsers.adapter = adapter
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditDialog(user: User) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_user, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.et_nombre)

        etNombre.setText(user.nombre)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Usuario")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newNombre = etNombre.text.toString().trim()

                if (newNombre.isEmpty()) {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.updateUser(user.id, newNombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(user: User) {
        if (user.rol == UserRole.ADMIN) {
            Toast.makeText(context, "No se puede eliminar al administrador", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar a ${user.nombre}?\n\nEsta acción no se puede deshacer.")
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