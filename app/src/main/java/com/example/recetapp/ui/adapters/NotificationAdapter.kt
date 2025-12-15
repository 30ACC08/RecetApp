package com.example.recetapp.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recetapp.R
import com.example.recetapp.data.model.Notification
import com.example.recetapp.data.model.NotificationType
import com.example.recetapp.databinding.ItemNotificationBinding

class NotificationAdapter : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message

            try {
                val date = DateUtils.getRelativeTimeSpanString(
                    notification.timestamp.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvDate.text = date
            } catch (e: Exception) {
                binding.tvDate.text = "Reciente"
            }

            val iconRes = when(notification.type) {
                NotificationType.FOLLOW -> R.drawable.ic_person
                NotificationType.REVIEW -> R.drawable.ic_search
                NotificationType.LIKE -> R.drawable.ic_favorite
                else -> R.drawable.ic_search
            }
            binding.ivTypeIcon.setImageResource(iconRes)

            if (notification.fromUserPhotoUrl.isNotEmpty()) {
                Glide.with(binding.root).load(notification.fromUserPhotoUrl).circleCrop().into(binding.ivUserPhoto)
            } else {
                binding.ivUserPhoto.setImageResource(R.drawable.ic_person)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification) = oldItem == newItem
    }
}