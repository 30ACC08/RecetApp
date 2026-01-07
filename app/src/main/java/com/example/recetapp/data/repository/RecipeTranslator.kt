package com.example.recetapp.data.repository

import android.util.Log
import com.example.recetapp.data.model.Ingredient
import com.example.recetapp.data.model.Recipe
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class RecipeTranslator {

    // Inicialización perezosa (Lazy) para evitar crasheos al arrancar la app
    private val translator by lazy {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build()
            Translation.getClient(options)
        } catch (e: Exception) {
            null
        }
    }

    private var isModelDownloaded = false

    suspend fun prepareModel() {
        if (isModelDownloaded || translator == null) return
        try {
            val conditions = DownloadConditions.Builder().build()
            translator?.downloadModelIfNeeded(conditions)?.await()
            isModelDownloaded = true
        } catch (e: Exception) {
            Log.e("Translator", "Error bajando modelo", e)
        }
    }

    suspend fun translateRecipe(recipe: Recipe): Recipe {
        // Si el traductor falló al iniciar, devolvemos la receta original sin cerrar la app
        val safeTranslator = translator ?: return recipe

        if (!isModelDownloaded) {
            try { prepareModel() } catch (e: Exception) { return recipe }
        }

        return try {
            val instruccionesEsp = formatAndTranslateInstructions(recipe.instructions, safeTranslator)

            val ingredientesEsp = recipe.ingredients.map { ing ->
                try {
                    val nombre = safeTranslator.translate(ing.name).await()
                    Ingredient(nombre.replaceFirstChar { it.uppercase() }, ing.measure)
                } catch (e: Exception) { ing }
            }

            recipe.copy(instructions = instruccionesEsp, ingredients = ingredientesEsp)
        } catch (e: Exception) {
            recipe
        }
    }

    suspend fun formatRecipeInstructions(recipe: Recipe): Recipe {
        // Formateo seguro sin traducción
        val formattedInstructions = processTextToSteps(recipe.instructions, null)
        return recipe.copy(instructions = formattedInstructions)
    }

    private suspend fun formatAndTranslateInstructions(text: String, translatorInstance: com.google.mlkit.nl.translate.Translator): String {
        return processTextToSteps(text, translatorInstance)
    }

    private suspend fun processTextToSteps(text: String, translatorInstance: com.google.mlkit.nl.translate.Translator?): String {
        if (text.isBlank()) return ""

        val hasNewlines = text.contains("\n") || text.contains("\r")
        val cleanText = if (!hasNewlines) text.replace(". ", ".\n") else text

        val rawSteps = cleanText.split(Regex("[\n\r]+"))
        val sb = StringBuilder()
        var counter = 1

        for (step in rawSteps) {
            val s = step.trim()
            if (s.length > 2) {
                var finalStep = s.replace(Regex("^\\d+[.)]\\s*"), "")

                if (translatorInstance != null) {
                    try {
                        finalStep = translatorInstance.translate(finalStep).await()
                    } catch(e: Exception) { /* Ignorar error traducción */ }
                }

                finalStep = finalStep.replaceFirstChar { it.uppercase() }
                if (!finalStep.endsWith(".")) finalStep += "."

                sb.append("$counter. $finalStep\n\n")
                counter++
            }
        }
        return sb.toString().trim()
    }
}