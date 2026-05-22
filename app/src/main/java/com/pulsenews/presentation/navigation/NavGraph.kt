package com.pulsenews.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pulsenews.presentation.screen.settings.SettingsScreen
import com.pulsenews.presentation.screen.subscriptions.SubscriptionScreen


@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Subscription.route
    ) {
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onNavigationToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }

}

sealed class Screen(val route: String) {

    data object Subscription : Screen("subscriptions")

    data object Settings : Screen("settings")
}