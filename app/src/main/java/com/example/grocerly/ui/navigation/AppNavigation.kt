package com.example.grocerly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.grocerly.ui.fragments.Home
import com.example.grocerly.ui.fragments.Login
import com.example.grocerly.ui.screen.HomeScreen
import com.example.grocerly.ui.screen.SplashScreen


@Composable
fun AppNavigation(isSkippedSplash: Boolean,
                  notificationOrderId: String? = null,
                  notificationProductId: String? = null){

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash"){

        composable("splash"){
            SplashScreen(
                isSkipped = isSkippedSplash,
                onNavigationToHome = {
                    navController.navigate("home"){
                        popUpTo("splash"){inclusive = true}
                    }
                },
                onNavigationToLogin = {
                    navController.navigate("login"){
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            Login()
        }

        composable("home") {

                HomeScreen(
                    onNavigateToCart = {
                        navController.navigate("cart") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSeeAll = {
                        navController.navigate("search") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCategory = { category ->
                        navController.navigate("search/${category.categoryName}") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAddAddress = {
                        navController.navigate("addAddress") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToUpdateAddress = { address ->
                        navController.navigate("updateAddress") {
                            launchSingleTop = true
                        }
                    },
                    onActionToOrderDetails = { cartProduct, order ->
                        navController.navigate("orderDetails") {
                            launchSingleTop = true
                        }
                    },
                    notificationOrderId = null,
                    notificationProductId = null
                )
        }

    }

}