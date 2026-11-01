package com.olab.studyqb.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isReview: Boolean = false)

private val tabs = listOf(
    Routes.ANALYZE to BottomTab("Analyze", Icons.Filled.Search),
    Routes.CARDS to BottomTab("Cards", Icons.AutoMirrored.Filled.Notes),
    "review" to BottomTab("Review", Icons.Filled.Timer, isReview = true),
    Routes.STATS to BottomTab("Stats", Icons.Filled.BarChart),
    Routes.SETTINGS to BottomTab("Settings", Icons.Filled.Settings)
)

@Composable
fun StudyQBBottomNavBar(navController: NavController, reviewResetKey: MutableIntState) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar {
        tabs.forEach { (baseRoute, tab) ->
            val selected = currentRoute?.hierarchy?.any {
                if (tab.isReview) it.route?.startsWith("review/") == true else it.route == baseRoute
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (tab.isReview) {
                        // Always push a fresh Review entry (new resetKey), even if already selected,
                        // to drop stale manually-selected-card references — matching iOS's
                        // `.id(reviewId)` reset-on-reselect behavior.
                        reviewResetKey.intValue += 1
                        navController.navigate(Routes.review(reviewResetKey.intValue)) {
                            popUpTo(Routes.ANALYZE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(baseRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
