package com.gymlog.app.domain.model

import java.util.UUID

data class Set(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val series: Int,
    val reps: Int,
    val weightKg: Float
)