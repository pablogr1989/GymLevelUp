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
    
    object CreateExercise : Screen(route = "create_exercise")
}
