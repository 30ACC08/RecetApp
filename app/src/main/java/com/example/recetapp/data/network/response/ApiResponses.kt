package com.example.recetapp.data.network.response

import com.google.gson.annotations.SerializedName

// ==================== TheMealDB Responses ====================

data class MealDbResponse(
    val meals: List<MealDbRecipe>?
)

data class MealDbRecipe(
    @SerializedName("idMeal") val id: String,
    @SerializedName("strMeal") val name: String,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?,
    @SerializedName("strInstructions") val instructions: String?,
    @SerializedName("strMealThumb") val thumbnail: String?,
    @SerializedName("strYoutube") val youtubeUrl: String?,
    @SerializedName("strSource") val sourceUrl: String?,
    @SerializedName("strTags") val tags: String?,

    // Ingredientes (hasta 20)
    @SerializedName("strIngredient1") val ingredient1: String?,
    @SerializedName("strIngredient2") val ingredient2: String?,
    @SerializedName("strIngredient3") val ingredient3: String?,
    @SerializedName("strIngredient4") val ingredient4: String?,
    @SerializedName("strIngredient5") val ingredient5: String?,
    @SerializedName("strIngredient6") val ingredient6: String?,
    @SerializedName("strIngredient7") val ingredient7: String?,
    @SerializedName("strIngredient8") val ingredient8: String?,
    @SerializedName("strIngredient9") val ingredient9: String?,
    @SerializedName("strIngredient10") val ingredient10: String?,
    @SerializedName("strIngredient11") val ingredient11: String?,
    @SerializedName("strIngredient12") val ingredient12: String?,
    @SerializedName("strIngredient13") val ingredient13: String?,
    @SerializedName("strIngredient14") val ingredient14: String?,
    @SerializedName("strIngredient15") val ingredient15: String?,
    @SerializedName("strIngredient16") val ingredient16: String?,
    @SerializedName("strIngredient17") val ingredient17: String?,
    @SerializedName("strIngredient18") val ingredient18: String?,
    @SerializedName("strIngredient19") val ingredient19: String?,
    @SerializedName("strIngredient20") val ingredient20: String?,

    // Medidas
    @SerializedName("strMeasure1") val measure1: String?,
    @SerializedName("strMeasure2") val measure2: String?,
    @SerializedName("strMeasure3") val measure3: String?,
    @SerializedName("strMeasure4") val measure4: String?,
    @SerializedName("strMeasure5") val measure5: String?,
    @SerializedName("strMeasure6") val measure6: String?,
    @SerializedName("strMeasure7") val measure7: String?,
    @SerializedName("strMeasure8") val measure8: String?,
    @SerializedName("strMeasure9") val measure9: String?,
    @SerializedName("strMeasure10") val measure10: String?,
    @SerializedName("strMeasure11") val measure11: String?,
    @SerializedName("strMeasure12") val measure12: String?,
    @SerializedName("strMeasure13") val measure13: String?,
    @SerializedName("strMeasure14") val measure14: String?,
    @SerializedName("strMeasure15") val measure15: String?,
    @SerializedName("strMeasure16") val measure16: String?,
    @SerializedName("strMeasure17") val measure17: String?,
    @SerializedName("strMeasure18") val measure18: String?,
    @SerializedName("strMeasure19") val measure19: String?,
    @SerializedName("strMeasure20") val measure20: String?
)

data class MealDbCategoriesResponse(
    val categories: List<MealDbCategory>?
)

data class MealDbCategory(
    @SerializedName("idCategory") val id: String,
    @SerializedName("strCategory") val name: String,
    @SerializedName("strCategoryThumb") val thumbnail: String?,
    @SerializedName("strCategoryDescription") val description: String?
)

// ==================== Spoonacular Responses ====================

data class SpoonacularSearchResponse(
    val results: List<SpoonacularRecipe>?,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

data class SpoonacularRecipe(
    val id: Int,
    val title: String,
    val image: String?,
    val imageType: String?,

    // Información adicional (si se incluye en la búsqueda)
    val readyInMinutes: Int?,
    val servings: Int?,
    val sourceUrl: String?,
    val healthScore: Double?,
    val pricePerServing: Double?,
    val cheap: Boolean?,
    val vegetarian: Boolean?,
    val vegan: Boolean?,
    val glutenFree: Boolean?,
    val dairyFree: Boolean?,
    val veryHealthy: Boolean?,
    val sustainable: Boolean?,
    val cuisines: List<String>?,
    val dishTypes: List<String>?,
    val summary: String?,
    val nutrition: SpoonacularNutrition?
)

data class SpoonacularRecipeDetail(
    val id: Int,
    val title: String,
    val image: String?,
    val readyInMinutes: Int,
    val servings: Int,
    val sourceUrl: String?,
    val healthScore: Double?,
    val pricePerServing: Double?,
    val cheap: Boolean,
    val vegetarian: Boolean,
    val vegan: Boolean,
    val glutenFree: Boolean,
    val dairyFree: Boolean,
    val veryHealthy: Boolean,
    val sustainable: Boolean,
    val cuisines: List<String>,
    val dishTypes: List<String>,
    val instructions: String?,
    val summary: String,
    val extendedIngredients: List<SpoonacularIngredient>,
    val nutrition: SpoonacularNutrition?
)

data class SpoonacularIngredient(
    val id: Int,
    val name: String,
    val amount: Double,
    val unit: String,
    val original: String
)

data class SpoonacularNutrition(
    val nutrients: List<SpoonacularNutrient>
) {
    fun getCalories(): Double? = nutrients.find { it.name == "Calories" }?.amount
    fun getProtein(): Double? = nutrients.find { it.name == "Protein" }?.amount
    fun getFat(): Double? = nutrients.find { it.name == "Fat" }?.amount
    fun getCarbs(): Double? = nutrients.find { it.name == "Carbohydrates" }?.amount
}

data class SpoonacularNutrient(
    val name: String,
    val amount: Double,
    val unit: String
)