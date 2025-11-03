package com.gymlog.app.domain.model

import com.gymlog.app.data.local.entity.MuscleGroup

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val muscleGroup: MuscleGroup,
    val imageUri: String?,
    val currentSeries: Int,
    val currentReps: Int,
    val currentWeightKg: Float,
    val createdAt: Long
)
