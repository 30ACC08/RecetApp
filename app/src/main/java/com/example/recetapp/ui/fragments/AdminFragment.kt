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

        android.util.Log.d("AdminFragment", "=== AdminFragment iniciado ===")

        lifecycleScope.launch {
            viewModel.loadCurrentUser()
        }

        setupClickListeners()
        setupObservers()

        // Cargar usuarios
        android.util.Log.d("AdminFragment", "Solicitando carga de usuarios...")
        viewModel.loadAllUsers()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        // Verificar permisos de admin
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            android.util.Log.d("AdminFragment", "Usuario actual: ${user?.nombre}, Rol: ${user?.rol}")

            if (user?.rol != UserRole.ADMIN) {
                android.util.Log.e("AdminFragment", "❌ Usuario no es admin")
                Toast.makeText(
                    context,
                    "No tienes permisos de administrador",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            } else {
                android.util.Log.d("AdminFragment", "✅ Usuario admin verificado")
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            android.util.Log.d("AdminFragment", "Estado de carga: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar lista de usuarios
        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            android.util.Log.d("AdminFragment", "=== USUARIOS RECIBIDOS ===")
            android.util.Log.d("AdminFragment", "Total: ${users.size}")

            binding.tvTotalUsuarios.text = users.size.toString()

            if (users.isEmpty()) {
                android.util.Log.d("AdminFragment", "Lista vacía, mostrando empty state")
                binding.llEmptyState.visibility = View.VISIBLE
                binding.recyclerViewUsers.visibility = View.GONE
            } else {
                android.util.Log.d("AdminFragment", "Mostrando lista de ${users.size} usuarios")
                binding.llEmptyState.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.VISIBLE

                // Log detallado de cada usuario
                users.forEachIndexed { index, user ->
                    android.util.Log.d("AdminFragment", "Usuario #${index + 1}:")
                    android.util.Log.d("AdminFragment", "  - Nombre: ${user.nombre}")
                    android.util.Log.d("AdminFragment", "  - Email: ${user.email}")
                    android.util.Log.d("AdminFragment", "  - Rol: ${user.rol}")
                    android.util.Log.d("AdminFragment", "  - ID: ${user.id}")
                }

                val adapter = UsersAdapter(
                    users = users,
                    isAdmin = true,
                    onEditClick = { user -> showEditDialog(user) },
                    onDeleteClick = { user -> showDeleteConfirmation(user) }
                )

                binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
                binding.recyclerViewUsers.adapter = adapter

                android.util.Log.d("AdminFragment", "✅ Adapter configurado")
            }
        }

        // Observar resultado de actualización
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                android.util.Log.d("AdminFragment", "✅ Usuario actualizado: ${user.nombre}")
                Toast.makeText(context, "Usuario actualizado", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.util.Log.e("AdminFragment", "❌ Error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar resultado de eliminación
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                android.util.Log.d("AdminFragment", "✅ Usuario eliminado")
                Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.util.Log.e("AdminFragment", "❌ Error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                if (newNombre.isNotEmpty() && newNombre.length >= 3) {
                    viewModel.updateUser(user.id, newNombre)
                } else {
                    Toast.makeText(context, "Nombre inválido", Toast.LENGTH_SHORT).show()
                }
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