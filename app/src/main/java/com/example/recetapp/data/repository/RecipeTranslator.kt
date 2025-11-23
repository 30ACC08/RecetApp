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

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()

    private val translator = Translation.getClient(options)
    private var isModelDownloaded = false

    suspend fun prepareModel() {
        if (isModelDownloaded) return
        try {
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            isModelDownloaded = true
        } catch (e: Exception) {
            Log.e("Translator", "Error bajando modelo", e)
        }
    }

    // 1. TRADUCIR Y FORMATEAR (Para mostrar en español)
    suspend fun translateRecipe(recipe: Recipe): Recipe {
        if (!isModelDownloaded) {
            try { prepareModel() } catch (e: Exception) { return recipe }
        }

        return try {
            // Instrucciones: Traducir y formatear
            val instruccionesEsp = formatAndTranslateInstructions(recipe.instructions)

            // Ingredientes: Traducir
            val ingredientesEsp = recipe.ingredients.map { ing ->
                try {
                    val nombre = translator.translate(ing.name).await()
                    Ingredient(nombre.replaceFirstChar { it.uppercase() }, ing.measure)
                } catch (e: Exception) { ing }
            }

            recipe.copy(
                instructions = instruccionesEsp,
                ingredients = ingredientesEsp
            )
        } catch (e: Exception) {
            recipe
        }
    }

    // 2. SOLO FORMATEAR (Para mostrar el original en inglés bien ordenado)
    suspend fun formatRecipeInstructions(recipe: Recipe): Recipe {
        // Llamamos a la función interna SIN activar la traducción
        val formattedInstructions = processTextToSteps(recipe.instructions, translate = false)
        return recipe.copy(instructions = formattedInstructions)
    }

    // Función interna que maneja la lógica
    private suspend fun formatAndTranslateInstructions(text: String): String {
        return processTextToSteps(text, translate = true)
    }

    // ÚNICA FUNCIÓN DE PROCESAMIENTO (Para evitar conflictos)
    private suspend fun processTextToSteps(text: String, translate: Boolean): String {
        if (text.isBlank()) return ""

        // Detectar saltos de línea o puntos
        val hasNewlines = text.contains("\n") || text.contains("\r")

        // Limpieza inicial para uniformidad
        val cleanText = if (!hasNewlines) text.replace(". ", ".\n") else text

        val rawSteps = cleanText.split(Regex("[\n\r]+"))
        val sb = StringBuilder()
        var counter = 1

        for (step in rawSteps) {
            val s = step.trim()
            if (s.length > 2) {
                // Quitar números antiguos (ej: "1. Cut") para re-numerar nosotros
                var finalStep = s.replace(Regex("^\\d+[.)]\\s*"), "")

                if (translate) {
                    try {
                        finalStep = translator.translate(finalStep).await()
                    } catch(e: Exception) {
                        // Si falla, se queda el texto en inglés limpio
                    }
                }

                // Capitalizar y asegurar punto final
                finalStep = finalStep.replaceFirstChar { it.uppercase() }
                if (!finalStep.endsWith(".")) finalStep += "."

                sb.append("$counter. $finalStep\n\n")
                counter++
            }
        }
        return sb.toString().trim()
    }
}