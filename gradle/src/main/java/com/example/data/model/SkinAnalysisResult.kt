package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SkinAnalysisResult(
    val skin_type: String,
    val issue: String,
    val confidence: Int,
    val description: String,
    val ingredients: List<String>,
    val routine: SkinRoutine,
    val metrics: SkinMetrics
)

@JsonClass(generateAdapter = true)
data class SkinRoutine(
    val morning: List<String>,
    val evening: List<String>
)

@JsonClass(generateAdapter = true)
data class SkinMetrics(
    val hydration: Int,
    val redness: Int,
    val texture: Int,
    val pores: Int
)
