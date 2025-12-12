package com.gymlog.app.domain.model

import com.gymlog.app.data.local.entity.DayOfWeek
import com.gymlog.app.data.local.entity.DayCategory

data class Calendar(
    val id: String,
    val name: String,
    val createdAt: Long
)

data class Month(
    val id: String,
    val calendarId: String,
    val name: String,
    val monthNumber: Int
)

data class Week(
    val id: String,
    val monthId: String,
    val weekNumber: Int
)

// NUEVO MODELO PARA LA ASIGNACIÓN
data class TrainingAssignment(
    val exerciseId: String,
    val targetSetId: String? = null // Nullable: puede ser el ejercicio base sin set específico
)

data class DaySlot(
    val id: String,
    val weekId: String,
    val dayOfWeek: DayOfWeek,
    val categories: List<DayCategory> = emptyList(),
    // CAMBIO CRÍTICO: Usamos objetos tipados, no strings
    val exercises: List<TrainingAssignment> = emptyList(),
    val completed: Boolean = false
)

// Modelo compuesto para vista de calendario
data class CalendarWithMonths(
    val calendar: Calendar,
    val months: List<MonthWithWeeks>
)

data class MonthWithWeeks(
    val month: Month,
    val weeks: List<WeekWithDays>
)

data class WeekWithDays(
    val week: Week,
    val days: List<DaySlot>
)