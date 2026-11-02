package com.example.taskmanager.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

@Composable
fun TaskApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "task_list") {
        composable("task_list") {
            TaskListScreen(
                navigateToTaskEntry = { navController.navigate("task_entry") },
                navigateToTaskUpdate = { id -> navController.navigate("task_update/$id") },
                navigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("task_entry") {
            TaskEntryScreen(navigateBack = { navController.popBackStack() })
        }
        composable(
            route = "task_update/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.IntType }),
            deepLinks = listOf(navDeepLink { uriPattern = "taskmanager://task/{taskId}" })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
            TaskUpdateScreen(
                taskId = taskId,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(navigateBack = { navController.popBackStack() })
        }
    }
}