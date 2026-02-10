package com.wifield.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Proyectos", Icons.Filled.Home)
    data object Scanner : Screen("scanner", "Scanner", Icons.Filled.Wifi)
    data object ActiveDiag : Screen("active", "Diagnóstico", Icons.Filled.NetworkCheck)
    data object Project : Screen("project/{projectId}", "Proyecto") {
        fun createRoute(projectId: Long) = "project/$projectId"
    }
    data object SnapshotDetail : Screen("snapshot/{snapshotId}", "Snapshot") {
        fun createRoute(snapshotId: Long) = "snapshot/$snapshotId"
    }
    data object Comparator : Screen("comparator/{projectId}", "Comparador") {
        fun createRoute(projectId: Long) = "comparator/$projectId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Scanner, Screen.ActiveDiag)
