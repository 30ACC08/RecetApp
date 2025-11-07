package com.example.recetapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.databinding.ItemUserBinding
import com.example.recetapp.data.model.User
import com.example.recetapp.data.model.UserRole

class UsersAdapter(
    private val users: List<User>,
    private val isAdmin: Boolean,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, position: Int) {
            binding.tvUserNumber.text = "Usuario #${position + 1}"
            binding.tvUserName.text = user.nombre
            binding.tvUserEmail.text = user.email
            binding.tvUserId.text = "ID: ${user.id.take(8)}..."

            // Mostrar badge de rol
            if (user.rol == UserRole.ADMIN) {
                binding.tvUserRole.visibility = View.VISIBLE
                binding.tvUserRole.text = "ADMIN"
            } else {
                binding.tvUserRole.visibility = View.GONE
            }

            // Mostrar botones solo si es admin
            if (isAdmin) {
                binding.llActions.visibility = View.VISIBLE

                binding.btnEdit.setOnClickListener {
                    onEditClick(user)
                }

                binding.btnDelete.setOnClickListener {
                    onDeleteClick(user)
                }
            } else {
                binding.llActions.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], position)
    }

    override fun getItemCount() = users.size
}