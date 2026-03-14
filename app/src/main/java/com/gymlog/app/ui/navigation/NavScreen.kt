package com.gymlog.app.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    object Main : Screen(route = "main")

    object ExerciseDetail : Screen(
        route = "exercise_detail/{exerciseId}",
        arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
    ) {
        fun createRoute(exerciseId: String) = "exercise_detail/$exerciseId"
    }

    object EditExercise : Screen(
        route = "edit_exercise/{exerciseId}",
        arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
    ) {
        fun createRoute(exerciseId: String) = "edit_exercise/$exerciseId"
    }

    object EditSet : Screen(
        route = "edit_set/{exerciseId}?setId={setId}",
        arguments = listOf(
            navArgument("exerciseId") { type = NavType.StringType },
            navArgument("setId") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) {
        fun createRoute(exerciseId: String, setId: String? = null): String {
            return if (setId != null) "edit_set/$exerciseId?setId=$setId" else "edit_set/$exerciseId"
        }
    }

    object CreateExercise : Screen(route = "create_exercise")
    object CalendarsList : Screen(route = "calendars_list")

    object CalendarDetail : Screen(
        route = "calendar_detail/{calendarId}",
        arguments = listOf(navArgument("calendarId") { type = NavType.StringType })
    ) {
        fun createRoute(calendarId: String) = "calendar_detail/$calendarId"
    }

    object DaySlotDetail : Screen(
        route = "day_slot_detail/{daySlotId}",
        arguments = listOf(navArgument("daySlotId") { type = NavType.StringType })
    ) {
        fun createRoute(daySlotId: String) = "day_slot_detail/$daySlotId"
    }

    object CreateCalendar : Screen(route = "create_calendar")

    object TrainingMode : Screen(
        route = "training_mode/{daySlotId}",
        arguments = listOf(navArgument("daySlotId") { type = NavType.StringType })
    ) {
        fun createRoute(daySlotId: String) = "training_mode/$daySlotId"
    }

    object Timer : Screen(route = "timer")
    object Backup : Screen(route = "backup")
    object WorkoutHistory : Screen(route = "workout_history")

    // NUEVA RUTA PARA EXPORTAR EL HISTORIAL
    object ExportHistory : Screen(route = "export_history")
}