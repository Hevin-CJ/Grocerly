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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.grocerly.ui.screen.CartScreen
import com.example.grocerly.ui.screen.CustomSearchScreen
import com.example.grocerly.ui.screen.FavouritesScreen
import com.example.grocerly.ui.screen.HomeScreen
import com.example.grocerly.ui.screen.LoginScreen
import com.example.grocerly.ui.screen.SplashScreen
import com.example.grocerly.utils.ProductCategory

@Composable
fun AppNavigation(
    isSkippedSplash: Boolean,
    notificationOrderId: String? = null,
    notificationProductId: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val searchRoute = "search?category={category}"
    val bottomBarRoutes = listOf("home", "favourites", searchRoute, "menu", "profile")
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "auth_graph",
            modifier = Modifier.padding(innerPadding)
        ) {

            navigation(
                startDestination = if (isSkippedSplash) "login" else "splash",
                route = "auth_graph"
            ) {
                composable("splash") {
                    SplashScreen(
                        isSkipped = isSkippedSplash,
                        onNavigationToHome = {
                            navController.navigate("home_graph") {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
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
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("home_graph") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToSignUp = {}
                    )
                }
            }

            navigation(startDestination = "home", route = "home_graph") {
                composable("home") {
                    HomeScreen(
                        onNavigateToCart = {
                            navController.navigate("cart") { launchSingleTop = true }
                        },
                        onNavigateToSeeAll = { productCategory ->
                            val route = if (productCategory != ProductCategory.selectcatgory) {
                                "search?category=${productCategory.name}"
                            } else {
                                "search?category="
                            }
                            navController.navigate(route) { launchSingleTop = true }
                        },
                        onNavigateToCategory = { parentCategory ->
                            navController.navigate("search?category=${parentCategory.category.name}") {
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

                composable("cart") {
                    CartScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCheckout = { navController.navigate("checkout") { launchSingleTop = true } }
                    )
                }

                composable(
                    route = "search?category={category}",
                    arguments = listOf(
                        navArgument("category") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val categoryArg = backStackEntry.arguments?.getString("category")
                    val productCategory = categoryArg?.let { arg ->
                        ProductCategory.fromString(arg)
                    }

                    CustomSearchScreen(
                        productCategory = productCategory,
                        onNavigateBack = { navController.popBackStack() }
                    )
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
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
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
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
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
                },
                colors = NavigationBarItemDefaults.colors(
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