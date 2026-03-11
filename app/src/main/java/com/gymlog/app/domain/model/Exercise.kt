package com.gymlog.app.domain.model

import com.gymlog.app.data.local.entity.MuscleGroup

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val muscleGroup: MuscleGroup,
    val imageUri: String?,
    val sets: List<Set> = emptyList(),
    val notes: String = "",
    val changeLogText: String = "",
    val createdAt: Long
) {
    // Propiedades calculadas para compatibilidad con UI existente
    // Toman el valor del primer set o 0 si no hay sets
    val currentSeries: Int
        get() = sets.firstOrNull()?.series ?: 0

    val currentMinReps: Int
        get() = sets.firstOrNull()?.minReps ?: 0

    val currentMaxReps: Int
        get() = sets.firstOrNull()?.maxReps ?: 0

    val currentWeightKg: Float
        get() = sets.firstOrNull()?.weightKg ?: 0f

    val currentMinRir: Int?
        get() = sets.firstOrNull()?.minRir

    val currentMaxRir: Int?
        get() = sets.firstOrNull()?.maxRir
}