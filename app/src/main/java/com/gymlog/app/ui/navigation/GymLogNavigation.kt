package com.gymlog.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymlog.app.ui.screens.create.CreateExerciseScreen
import com.gymlog.app.ui.screens.detail.ExerciseDetailScreen
import com.gymlog.app.ui.screens.main.MainScreen

@Composable
fun GymLogNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(route = Screen.Main.route) {
            MainScreen(
                onNavigateToDetail = { exerciseId ->
                    navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateExercise.route)
                }
            )
        }
        
        composable(
            route = Screen.ExerciseDetail.route,
            arguments = Screen.ExerciseDetail.arguments
        ) {
            ExerciseDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(route = Screen.CreateExercise.route) {
            CreateExerciseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
