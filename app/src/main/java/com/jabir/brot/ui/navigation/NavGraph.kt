package com.jabir.brot.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jabir.brot.ui.screens.*
import com.jabir.brot.ui.viewmodel.*

import com.jabir.brot.ReelCountApp

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userPrefs = ReelCountApp.instance.userPreferencesRepository
    val isDeveloperMode by userPrefs.developerMode.collectAsState(initial = false)

    val navItems = remember(isDeveloperMode) {
        if (isDeveloperMode) {
            listOf(
                Screen.Dashboard,
                Screen.Statistics,
                Screen.Goals,
                Screen.Developer,
                Screen.Settings
            )
        } else {
            listOf(
                Screen.Dashboard,
                Screen.Statistics,
                Screen.Goals,
                Screen.Settings
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel()
                DashboardScreen(viewModel = vm)
            }
            composable(Screen.Statistics.route) {
                val vm: StatisticsViewModel = viewModel()
                StatisticsScreen(viewModel = vm)
            }
            composable(Screen.Goals.route) {
                val vm: GoalsViewModel = viewModel()
                GoalsScreen(viewModel = vm)
            }
            composable(Screen.Developer.route) {
                DeveloperScreen()
            }
            composable(Screen.Diagnostics.route) {
                val vm: DiagnosticsViewModel = viewModel()
                DiagnosticsScreen(viewModel = vm)
            }
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel()
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
