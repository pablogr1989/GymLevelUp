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
        arguments = listOf(
            navArgument("exerciseId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(exerciseId: String): String {
            return "exercise_detail/$exerciseId"
        }
    }
    
    object EditExercise : Screen(
        route = "edit_exercise/{exerciseId}",
        arguments = listOf(
            navArgument("exerciseId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(exerciseId: String): String {
            return "edit_exercise/$exerciseId"
        }
    }
    
    object CreateExercise : Screen(route = "create_exercise")
    
    object CalendarsList : Screen(route = "calendars_list")
    
    object CalendarDetail : Screen(
        route = "calendar_detail/{calendarId}",
        arguments = listOf(
            navArgument("calendarId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(calendarId: String): String {
            return "calendar_detail/$calendarId"
        }
    }
    
    object CreateCalendar : Screen(route = "create_calendar")
    
    object Timer : Screen(route = "timer")
}
