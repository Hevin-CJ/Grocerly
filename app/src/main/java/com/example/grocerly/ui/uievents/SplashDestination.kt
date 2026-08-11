package com.example.grocerly.ui.uievents

sealed class SplashDestination {
    object Home : SplashDestination()
    object Login : SplashDestination()
}