package com.example.recetapp.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recetapp.data.model.Recipe
import com.example.recetapp.data.model.RecipeTranslations
import com.example.recetapp.databinding.FragmentCreateRecipeBinding
import com.example.recetapp.ui.viewmodel.CreateRecipeViewModel

class CreateRecipeFragment : Fragment() {

    private var _binding: FragmentCreateRecipeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateRecipeViewModel by viewModels()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri)
            binding.ivRecipe.setImageURI(uri)
            binding.llAddPhotoHint.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown()
        setupClickListeners()
        setupObservers()
        checkForEditMode()
    }

    private fun setupDropdown() {
        val categories = RecipeTranslations.CATEGORIES.values.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(adapter)
    }

    private fun checkForEditMode() {
        // Recibir receta si es edición (usamos argumentos del bundle)
        val recipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("recipe", Recipe::class.java)
        } else {
            arguments?.getParcelable("recipe")
        }

        if (recipe != null) {
            viewModel.loadRecipeForEdit(recipe)
            binding.tvTitle.text = "Editar Receta"
            binding.etName.setText(recipe.name)
            binding.etCategory.setText(RecipeTranslations.categoryName(recipe.category), false)
            binding.etTime.setText(recipe.readyInMinutes?.toString() ?: "")
            binding.etIngredients.setText(recipe.ingredients.joinToString("\n") { it.name })
            binding.etInstructions.setText(recipe.instructions)

            Glide.with(this).load(recipe.imageUrl).into(binding.ivRecipe)
            binding.llAddPhotoHint.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.cvImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSave.setOnClickListener {
            viewModel.saveRecipe(
                name = binding.etName.text.toString(),
                categoryEsp = binding.etCategory.text.toString(),
                ingredientsText = binding.etIngredients.text.toString(),
                instructions = binding.etInstructions.text.toString(),
                readyInMinutes = binding.etTime.text.toString()
            )
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.flLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }
        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "¡Guardado con éxito!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}