package com.example.grocerly.ui.navigation

import androidx.annotation.DrawableRes
import com.example.grocerly.R

sealed class BottomNavItem (
    val route: String,
    val title: String,
    @DrawableRes val icon: Int
){
    object Home: BottomNavItem("home", "Home", R.drawable.homebtm)
    object Favourites: BottomNavItem("favourites", "Favourites", R.drawable.favourites)
    object Search: BottomNavItem("search", "Search", R.drawable.search)
    object Profile: BottomNavItem("profile", "Profile", R.drawable.profile)
    object Menu: BottomNavItem("menu", "Menu", R.drawable.menubtm)

}