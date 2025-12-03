package com.gymlog.app.ui.util

import androidx.annotation.StringRes
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.data.local.entity.DayOfWeek
import com.gymlog.app.data.local.entity.MuscleGroup

/**
 * Clase utilitaria para mapear Enums de la base de datos a recursos de texto (Strings.xml).
 * Esto evita hardcodear textos en las pantallas y facilita la traducción.
 */
object UiMappers {

    @StringRes
    fun getDisplayNameRes(muscleGroup: MuscleGroup): Int {
        return when (muscleGroup) {
            MuscleGroup.LEGS -> R.string.muscle_legs
            MuscleGroup.GLUTES -> R.string.muscle_glutes
            MuscleGroup.BACK -> R.string.muscle_back
            MuscleGroup.CHEST -> R.string.muscle_chest
            MuscleGroup.BICEPS -> R.string.muscle_biceps
            MuscleGroup.TRICEPS -> R.string.muscle_triceps
            MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        }
    }

    @StringRes
    fun getDisplayNameRes(category: DayCategory): Int {
        return when (category) {
            DayCategory.FULL_BODY -> R.string.cat_full_body
            DayCategory.CHEST -> R.string.cat_chest
            DayCategory.LEGS -> R.string.cat_legs
            DayCategory.GLUTES -> R.string.cat_glutes
            DayCategory.BACK -> R.string.cat_back
            DayCategory.BICEPS -> R.string.cat_biceps
            DayCategory.TRICEPS -> R.string.cat_triceps
            DayCategory.SHOULDERS -> R.string.cat_shoulders
            DayCategory.CARDIO -> R.string.cat_cardio
            DayCategory.REST -> R.string.cat_rest
        }
    }

    @StringRes
    fun getDisplayNameRes(day: DayOfWeek): Int {
        return when (day) {
            DayOfWeek.MONDAY -> R.string.dow_monday
            DayOfWeek.TUESDAY -> R.string.dow_tuesday
            DayOfWeek.WEDNESDAY -> R.string.dow_wednesday
            DayOfWeek.THURSDAY -> R.string.dow_thursday
            DayOfWeek.FRIDAY -> R.string.dow_friday
            DayOfWeek.SATURDAY -> R.string.dow_saturday
            DayOfWeek.SUNDAY -> R.string.dow_sunday
        }
    }
}