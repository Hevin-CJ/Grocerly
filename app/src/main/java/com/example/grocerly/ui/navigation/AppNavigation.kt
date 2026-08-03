package com.example.grocerly.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.grocerly.ui.screen.FavouritesScreen
import com.example.grocerly.ui.screen.HomeScreen
import com.example.grocerly.ui.screen.SplashScreen


@Composable
fun AppNavigation(isSkippedSplash: Boolean,
                  notificationOrderId: String? = null,
                  notificationProductId: String? = null) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf("home","favourites", "search", "menu", "profile")
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(navController, currentRoute)
            }
        }

    ) {innerPadding->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(
                    isSkipped = isSkippedSplash,
                    onNavigationToHome = {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onNavigationToLogin = {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Login Screen Placeholder")
                }
            }



            composable("home") {
                HomeScreen(
                    onNavigateToCart = {
                        navController.navigate("cart") { launchSingleTop = true }
                    },
                    onNavigateToSeeAll = {
                        navController.navigate("search") { launchSingleTop = true }
                    },
                    onNavigateToCategory = { category ->
                        navController.navigate("search/${category.categoryName}") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAddAddress = {
                        navController.navigate("addAddress") { launchSingleTop = true }
                    },
                    onNavigateToUpdateAddress = { address ->
                        navController.navigate("updateAddress") { launchSingleTop = true }
                    },
                    onActionToOrderDetails = { cartProduct, order ->
                        navController.navigate("orderDetails") { launchSingleTop = true }
                    },
                    notificationOrderId = notificationOrderId,
                    notificationProductId = notificationProductId
                )
            }

            composable("favourites") {
                FavouritesScreen()
            }

            composable("search") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search Screen Placeholder")
                }
            }

            composable("menu") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Menu Screen Placeholder")
                }
            }

            composable("profile") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Profile Screen Placeholder")
                }
            }

        }
    }

}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
){
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Favourites,
        BottomNavItem.Search,
        BottomNavItem.Profile,
        BottomNavItem.Menu

    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach {item ->

            val isSelected = currentRoute==item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute!=item.route){
                        navController.navigate(item.route){
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp
                    )
                }, colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E7D32),
                        selectedTextColor = Color(0xFF2E7D32),
                        unselectedIconColor = Color.Black,
                        unselectedTextColor = Color.Black,
                        indicatorColor = Color(0xFFE8F5E9)
                        )
            )
        }

    }

}