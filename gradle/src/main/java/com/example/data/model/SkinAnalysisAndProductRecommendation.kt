package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SkincareProductSuggestion(
    val name: String,
    val brand: String,
    val purpose: String,
    val howToUse: String
)

@JsonClass(generateAdapter = true)
data class SkinAnalysisAndProductRecommendation(
    val detectedSkinType: String,
    val scientificAnalysis: String,
    val targetActiveIngredients: List<String>,
    val morningRoutineSteps: List<String>,
    val eveningRoutineSteps: List<String>,
    val protectiveAdvice: String,
    val suggestedProducts: List<SkincareProductSuggestion>,
    val avoidIngredientsAndTriggers: List<String>,
    val lifestyleRecommendations: List<String>
)
