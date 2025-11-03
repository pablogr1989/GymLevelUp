package com.gymlog.app.domain.model

data class ExerciseHistory(
    val id: String,
    val exerciseId: String,
    val timestamp: Long,
    val series: Int,
    val reps: Int,
    val weightKg: Float
)
