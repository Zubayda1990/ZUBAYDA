package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PersonalizedSkincareTip(
    val concern: String,
    val molecular_cause: String,
    val chemical_advice: String,
    val daily_habit: String,
    val critical_note: String
)
