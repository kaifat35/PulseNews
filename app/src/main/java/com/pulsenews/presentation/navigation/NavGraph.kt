package com.pulsenews.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulsenews.R
import com.pulsenews.presentation.screen.settings.SettingsScreen
import com.pulsenews.presentation.screen.subscriptions.ArticleWebViewScreen
import com.pulsenews.presentation.screen.subscriptions.FavoritesScreen
import com.pulsenews.presentation.screen.subscriptions.SubscriptionScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Нижние вкладки
enum class BottomNavScreen(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SUBSCRIPTIONS("subscriptions", R.string.subscriptions_title, Icons.Default.List),
    FAVORITES("favorites", R.string.favorites_title, Icons.Default.FavoriteBorder),
    SETTINGS("settings", R.string.settings_title, Icons.Default.Settings)
}

// Главный экран с нижней навигацией
@Composable
fun PulseNewsApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                BottomNavScreen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.labelRes)
                            )
                        },
                        label = { Text(stringResource(screen.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            startDestination = BottomNavScreen.SUBSCRIPTIONS.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}


@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Экран подписок
        composable(BottomNavScreen.SUBSCRIPTIONS.route) {
            SubscriptionScreen(
                onNavigationToSettings = {
                    navController.navigate(BottomNavScreen.SETTINGS.route)
                },
                onOpenArticle = { url ->
                    val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate("article/$encoded")
                },
                onNavigateToFavorites = {
                    navController.navigate(BottomNavScreen.FAVORITES.route)
                }
            )
        }

        // Экран избранного
        composable(BottomNavScreen.FAVORITES.route) {
            FavoritesScreen(
                onBackClick = {
                    navController.navigate(BottomNavScreen.SUBSCRIPTIONS.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onOpenArticle = { url ->
                    val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate("article/$encoded")
                }
            )
        }

        // Экран настроек
        composable(BottomNavScreen.SETTINGS.route) {
            SettingsScreen(
                onBackClick = {
                    navController.navigate(BottomNavScreen.SUBSCRIPTIONS.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // WebView для чтения статьи
        composable(
            route = "article/{url}",
            arguments = listOf(navArgument("url") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("url").orEmpty()
            val url = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
            ArticleWebViewScreen(url)
        }
    }
}