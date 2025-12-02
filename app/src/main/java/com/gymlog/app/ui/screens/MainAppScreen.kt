package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymlog.app.ui.screens.calendars.CalendarsListScreen
import com.gymlog.app.ui.screens.main.MainScreen
import com.gymlog.app.ui.screens.timer.TimerScreen
import com.gymlog.app.ui.navigation.Screen
import com.gymlog.app.ui.screens.backup.BackupScreen
import com.gymlog.app.ui.screens.calendars.CalendarDetailScreen
import com.gymlog.app.ui.screens.calendars.CreateCalendarScreen
import com.gymlog.app.ui.screens.calendars.DaySlotDetailScreen
import com.gymlog.app.ui.screens.create.CreateExerciseScreen
import com.gymlog.app.ui.screens.detail.ExerciseDetailScreen
import com.gymlog.app.ui.screens.edit.EditExerciseScreen
import com.gymlog.app.ui.screens.edit.EditSetScreen
import com.gymlog.app.ui.screens.training.TrainingModeScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Exercises : BottomNavItem("exercises_tab", "Ejercicios", Icons.Default.FitnessCenter)
    object Calendars : BottomNavItem("calendars_tab", "Calendarios", Icons.Default.CalendarMonth)
    object Timer : BottomNavItem("timer_tab", "Cronometro", Icons.Default.Timer)
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Exercises,
        BottomNavItem.Calendars,
        BottomNavItem.Timer
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Exercises.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.CreateExercise.route) {
                CreateExerciseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ExerciseDetail.route,
                arguments = Screen.ExerciseDetail.arguments
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""

                ExerciseDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.EditExercise.createRoute(id))
                    },
                    onNavigateToEditSet = { _, setId ->
                        navController.navigate(Screen.EditSet.createRoute(exerciseId, setId))
                    }
                )
            }

            composable(
                route = Screen.EditExercise.route,
                arguments = Screen.EditExercise.arguments
            ) {
                EditExerciseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditSet.route,
                arguments = Screen.EditSet.arguments
            ) {
                EditSetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CreateCalendar.route) {
                CreateCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.DaySlotDetail.route,
                arguments = Screen.DaySlotDetail.arguments
            ) {
                DaySlotDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExercise = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    },
                    onNavigateToTraining = { daySlotId ->
                        navController.navigate(Screen.TrainingMode.createRoute(daySlotId))
                    }
                )
            }

            composable(
                route = Screen.TrainingMode.route,
                arguments = Screen.TrainingMode.arguments
            ) {
                TrainingModeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CalendarDetail.route,
                arguments = Screen.CalendarDetail.arguments
            ) {
                CalendarDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { daySlotId ->
                        navController.navigate(Screen.DaySlotDetail.createRoute(daySlotId))
                    },
                    onNavigateToExercise = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    }
                )
            }


            composable(BottomNavItem.Exercises.route) {
                MainScreen(
                    onNavigateToDetail = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateExercise.route)
                    },
                    onNavigateToBackup = {
                        navController.navigate(Screen.Backup.route)
                    }
                )
            }

            composable(BottomNavItem.Calendars.route) {
                CalendarsListScreen(
                    onNavigateToDetail = { calendarId ->
                        navController.navigate(Screen.CalendarDetail.createRoute(calendarId))
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateCalendar.route)
                    }
                )
            }

            composable(BottomNavItem.Timer.route) {
                TimerScreen()
            }

            composable(Screen.Backup.route) {
                BackupScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}