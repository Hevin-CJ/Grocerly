package com.example.grocerly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerly.ui.uievents.SplashDestination
import com.example.grocerly.preferences.GrocerlyDataStore
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@HiltViewModel
class SplashViewModel @Inject constructor(private val grocerlyDataStore: GrocerlyDataStore,private val auth: FirebaseAuth): ViewModel() {

    val isLoggedIn: StateFlow<Boolean?> = grocerlyDataStore.getLoginState()
        .map { isDataStoreLoggedIn ->
            val hasActiveFirebaseSession = auth.currentUser != null
            hasActiveFirebaseSession && isDataStoreLoggedIn
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )




}