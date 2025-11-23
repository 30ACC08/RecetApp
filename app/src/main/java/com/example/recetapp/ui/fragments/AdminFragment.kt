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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch { viewModel.loadCurrentUser() }
        setupClickListeners()
        setupObservers()

        // Cargar usuarios al iniciar
        viewModel.loadAllUsers()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupObservers() {
        // 1. Verificar Rol (CORREGIDO: solo si user != null)
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                if (user.rol != UserRole.ADMIN) {
                    Toast.makeText(context, "Acceso denegado", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
        }

        // 2. Lista de Usuarios
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            binding.tvTotalUsuarios.text = users.size.toString()
            if (users.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUsers.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE

                val adapter = UsersAdapter(
                    users = users,
                    isAdmin = true,
                    onEditClick = { showEditDialog(it) },
                    onDeleteClick = { showDeleteConfirmation(it) }
                )
                binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
                binding.recyclerViewUsers.adapter = adapter
            }
        }

        // 3. Errores Generales (CORREGIDO: muestra por qué falla la carga)
        viewModel.generalError.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show() }
            result.onFailure { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show() }
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
                viewModel.updateUser(user.id, etNombre.text.toString().trim())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(user: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Eliminar a ${user.nombre}?")
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