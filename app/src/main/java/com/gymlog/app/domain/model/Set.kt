package com.gymlog.app.domain.model

import java.util.UUID

data class Set(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val series: Int,
    val minReps: Int,
    val maxReps: Int,
    val weightKg: Float,
    val minRir: Int? = null,
    val maxRir: Int? = null
)