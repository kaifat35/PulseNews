package com.pulsenews.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulsenews.presentation.screen.settings.SettingsScreen
import com.pulsenews.presentation.screen.subscriptions.FavoritesScreen
import com.pulsenews.presentation.screen.subscriptions.ArticleWebViewScreen
import com.pulsenews.presentation.screen.subscriptions.SubscriptionScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Subscription.route) {
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onNavigationToSettings = { navController.navigate(Screen.Settings.route) },
                onOpenArticle = { url -> navController.navigate("article/${URLEncoder.encode(url, StandardCharsets.UTF_8.toString())}") },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen(onBackClick = { navController.popBackStack() }) }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBackClick = { navController.popBackStack() },
                onOpenArticle = { url -> navController.navigate("article/${URLEncoder.encode(url, StandardCharsets.UTF_8.toString())}") }
            )
        }
        composable("article/{url}", arguments = listOf(navArgument("url") { type = NavType.StringType })) {
            val encoded = it.arguments?.getString("url").orEmpty()
            ArticleWebViewScreen(URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString()))
        }
    }
}

sealed class Screen(val route: String) {
    data object Subscription : Screen("subscriptions")
    data object Settings : Screen("settings")
    data object Favorites : Screen("favorites")
}
