package com.example.grocerly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.model.uievents.SplashDestination
import com.example.grocerly.preferences.GrocerlyDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@HiltViewModel
class SplashViewModel @Inject constructor(private val grocerlyDataStore: GrocerlyDataStore): ViewModel() {

    private val navigationSplash = MutableSharedFlow<SplashDestination>()
    val _navigationSplash = navigationSplash.asSharedFlow()

    fun checkAuthState(isSkippedSplash: Boolean){
       viewModelScope.launch {
           if (!isSkippedSplash){
               delay(3000L.milliseconds)
           }
           val isLoggedIn = grocerlyDataStore.getLoginState().first()
           if (isLoggedIn){
               navigationSplash.emit(SplashDestination.Home)
           }else{
               navigationSplash.emit(SplashDestination.Login)
           }
       }

    }



}