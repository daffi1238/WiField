package com.wifield.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wifield.app.ui.navigation.Screen
import com.wifield.app.ui.navigation.bottomNavItems
import com.wifield.app.ui.permission.PermissionRequestScreen
import com.wifield.app.ui.permission.getRequiredPermissions
import com.wifield.app.ui.screens.active.ActiveDiagnosticScreen
import com.wifield.app.ui.screens.comparator.ComparatorScreen
import com.wifield.app.ui.screens.home.HomeScreen
import com.wifield.app.ui.screens.project.ProjectScreen
import com.wifield.app.ui.screens.scanner.ScannerScreen
import com.wifield.app.ui.screens.snapshot.SnapshotDetailScreen
import com.wifield.app.ui.theme.WiFieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiFieldTheme {
                WiFieldApp()
            }
        }
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val nearbyWifi = ContextCompat.checkSelfPermission(
            context, Manifest.permission.NEARBY_WIFI_DEVICES
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || nearbyWifi
    }
    return fineLocation
}

@Composable
fun WiFieldApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermissions by remember { mutableStateOf(hasLocationPermission(context)) }
    val requiredPermissions = remember { getRequiredPermissions() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPermissions = hasLocationPermission(context)
    }

    if (!hasPermissions) {
        PermissionRequestScreen(
            onRequestPermissions = {
                permissionLauncher.launch(requiredPermissions)
            }
        )
    } else {
        WiFieldNavHost()
    }
}

@Composable
fun WiFieldNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.Project.createRoute(projectId))
                    }
                )
            }

            composable(Screen.Scanner.route) {
                ScannerScreen()
            }

            composable(Screen.ActiveDiag.route) {
                ActiveDiagnosticScreen()
            }

            composable(
                route = Screen.Project.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                ProjectScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onSnapshotClick = { snapshotId ->
                        navController.navigate(Screen.SnapshotDetail.createRoute(snapshotId))
                    },
                    onComparatorClick = { pid ->
                        navController.navigate(Screen.Comparator.createRoute(pid))
                    }
                )
            }

            composable(
                route = Screen.SnapshotDetail.route,
                arguments = listOf(navArgument("snapshotId") { type = NavType.LongType })
            ) { backStackEntry ->
                val snapshotId = backStackEntry.arguments?.getLong("snapshotId") ?: return@composable
                SnapshotDetailScreen(
                    snapshotId = snapshotId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Comparator.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                ComparatorScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
