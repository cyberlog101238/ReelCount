package com.jabir.brot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Today", Icons.Default.Home)
    object Statistics : Screen("statistics", "Progress", Icons.Default.BarChart)
    object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    object Developer : Screen("developer", "Developer", Icons.Default.Code)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Default.BugReport)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Onboarding : Screen("onboarding", "Setup", Icons.Default.Help)
}
