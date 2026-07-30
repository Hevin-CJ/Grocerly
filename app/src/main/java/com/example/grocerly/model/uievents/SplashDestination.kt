package com.example.grocerly.model.uievents

sealed class SplashDestination {
    object Home : SplashDestination()
    object Login : SplashDestination()
}