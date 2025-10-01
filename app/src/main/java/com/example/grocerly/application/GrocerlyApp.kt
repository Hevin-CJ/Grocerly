package com.example.grocerly.application

import android.app.Application
import android.content.Context
import com.example.grocerly.utils.LocaleUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.LocalCacheSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class GrocerlyApp:Application(){
    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().clearPersistence()
    }
}