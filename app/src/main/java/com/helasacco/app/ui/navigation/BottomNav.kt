package com.helasacco.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Routes.DASHBOARD, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Members", Routes.MEMBER_LIST, Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem("Loans", Routes.LOAN_LIST, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    BottomNavItem("Reports", Routes.REPORTS, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("More", Routes.SETTINGS, Icons.Filled.Menu, Icons.Outlined.Menu),
)

@Composable
fun HelaBottomBar(navController: NavController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}
