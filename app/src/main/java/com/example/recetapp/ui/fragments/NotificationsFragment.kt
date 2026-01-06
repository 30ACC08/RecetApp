package com.example.recetapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recetapp.databinding.FragmentNotificationsBinding
import com.example.recetapp.ui.adapters.NotificationAdapter
import com.example.recetapp.ui.viewmodel.AuthViewModel

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    // CORRECCIÓN: Pasamos 'notification.id' (String) porque el ViewModel espera un String
    private val adapter = NotificationAdapter { notification ->
        viewModel.markNotificationAsRead(notification.id)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón atrás (ID en XML: btn_back -> Binding: btnBack)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Configurar RecyclerView (ID en XML: rv_notifications -> Binding: rvNotifications)
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter

        // Cargar datos
        viewModel.loadNotifications()

        // Observadores
        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            // ID en XML: ll_empty -> Binding: llEmpty
            binding.llEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // ID en XML: progress_bar -> Binding: progressBar
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}