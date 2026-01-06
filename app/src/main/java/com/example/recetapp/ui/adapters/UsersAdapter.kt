package com.example.recetapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole
import com.example.recetapp.databinding.ItemUserBinding

class UsersAdapter(
    private val onUserClick: (User) -> Unit = {}, // Clic simple (ir a perfil)
    private val onEditClick: (User) -> Unit = {}, // Solo Admin
    private val onDeleteClick: (User) -> Unit = {}, // Solo Admin
    private val isAdminMode: Boolean = false // Flag para mostrar controles
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding, onUserClick, onEditClick, onDeleteClick, isAdminMode)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        private val binding: ItemUserBinding,
        private val onUserClick: (User) -> Unit,
        private val onEditClick: (User) -> Unit,
        private val onDeleteClick: (User) -> Unit,
        private val isAdminMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUserName.text = user.nombre
            binding.tvUserEmail.text = user.email
            binding.tvUserId.text = "ID: ${user.id}"

            // Mostrar rol si es admin
            if (user.rol == UserRole.ADMIN) {
                binding.tvUserRole.visibility = View.VISIBLE
                binding.tvUserRole.text = "ADMIN"
            } else {
                binding.tvUserRole.visibility = View.GONE
            }

            // Lógica de visualización según modo
            if (isAdminMode) {
                binding.llActions.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEditClick(user) }
                binding.btnDelete.setOnClickListener { onDeleteClick(user) }
                // En modo admin, el clic en la tarjeta no hace nada o edita
                binding.root.setOnClickListener(null)
            } else {
                binding.llActions.visibility = View.GONE
                binding.root.setOnClickListener { onUserClick(user) }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}