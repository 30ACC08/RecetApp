package com.example.recetapp.data.network

import com.example.recetapp.data.network.response.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ==================== TheMealDB API ====================

interface TheMealDbApi {

    @GET("random.php")
    suspend fun getRandomRecipe(): Response<MealDbResponse>

    @GET("search.php")
    suspend fun searchRecipes(
        @Query("s") query: String
    ): Response<MealDbResponse>

    @GET("lookup.php")
    suspend fun getRecipeById(
        @Query("i") id: String
    ): Response<MealDbResponse>

    @GET("filter.php")
    suspend fun getRecipesByCategory(
        @Query("c") category: String
    ): Response<MealDbResponse>

    @GET("filter.php")
    suspend fun getRecipesByArea(
        @Query("a") area: String
    ): Response<MealDbResponse>

    @GET("filter.php")
    suspend fun getRecipesByIngredient(
        @Query("i") ingredient: String
    ): Response<MealDbResponse>

    @GET("categories.php")
    suspend fun getCategories(): Response<MealDbCategoriesResponse>

    @GET("list.php?a=list")
    suspend fun getAreas(): Response<MealDbResponse>

    companion object {
        private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

        fun create(): TheMealDbApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TheMealDbApi::class.java)
        }
    }
}

// ==================== Spoonacular API ====================

interface SpoonacularApi {

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("diet") diet: String? = null,
        @Query("type") type: String? = null,
        @Query("maxReadyTime") maxReadyTime: Int? = null,
        @Query("minHealthScore") minHealthScore: Int? = null,
        @Query("maxCalories") maxCalories: Int? = null,
        @Query("number") number: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("addRecipeNutrition") addRecipeNutrition: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("sort") sort: String? = null
    ): Response<SpoonacularSearchResponse>

    @GET("recipes/{id}/information")
    suspend fun getRecipeById(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = true
    ): Response<SpoonacularRecipeDetail>

    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int = 10
    ): Response<SpoonacularRandomResponse>

    companion object {
        private const val BASE_URL = "https://api.spoonacular.com/"

        const val API_KEY = "ef363f431c6448f3b0504358c815ecfb"

        fun create(): SpoonacularApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpoonacularApi::class.java)
        }
    }
}

data class SpoonacularRandomResponse(
    val recipes: List<SpoonacularRecipeDetail>
)

// ==================== API Manager ====================

object ApiManager {
    val mealDbApi: TheMealDbApi by lazy { TheMealDbApi.create() }
    val spoonacularApi: SpoonacularApi by lazy { SpoonacularApi.create() }
}